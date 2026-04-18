import {useState, useEffect, useRef } from "react"
import { usernameFromToken } from '../api'

const audioDirectory = 'Audio'
const audioNames = [
    'cardPlace1',
    'cardPlace2',
    'cardPlace3',

    'cardSlide1',
    'cardSlide2',
    'cardSlide3',

    'chipsCollide1',
    'chipsCollide2',
    'chipsCollide3',

    'dieThrow1',
    'dieThrow2',
]

class Queue {
  constructor() {
    this.items = [];
  }

  enqueue(element) {
    this.items.push(element); 
  }

  dequeue() {
    return this.isEmpty() ? null : this.items.shift();
  }

  peek() {
    return this.isEmpty() ? null : this.items[0];
  }

  isEmpty() {
    return this.items.length === 0;
  }

  size() {
    return this.items.length;
  }
}

class Frame {
    constructor(image, lifetime = 0, [x,y] = [0,0], rotation = 0, [scaleX,scaleY] = [1,1]) {
        this.image = image;
        this.lifetime = lifetime;
        this.translation = [x,y];
        this.rotation = rotation;
        this.scale = [scaleX, scaleY];
        this.accumulator = 0;
    }

    draw(canvas, ctx) {
        ctx.save();
        
        const [x,y] = this.translation;
        const [scaleX, scaleY] = this.scale;

        const screenX = (x + 1) * canvas.width / 2;
        const screenY = (y + 1) * canvas.height / 2;

        const width = canvas.width;
        const height = canvas.height;
        const aspectRatio = width / height;

        const pixelWidth  = (scaleX / 2) * width  / aspectRatio;
        const pixelHeight = (scaleY / 2) * height;

        ctx.save();
        ctx.translate(screenX, screenY);
        ctx.rotate(this.rotation * Math.PI / 180);
        ctx.drawImage(this.image, -pixelWidth / 2, -pixelHeight / 2, pixelWidth, pixelHeight);
        ctx.restore();
    }
}

class Animation {
    constructor(frames, loopcount = 1, onBegin = null, onEnd = null) {
        this.frames = frames;
        this.currentFrameIdx = 0;
        this.loopcount = loopcount;
        this.onBegin = onBegin;
        this.onEnd = onEnd;
    }

    finished() {
        return this.currentFrameIdx === this.frames.length - 1;
    }

    update(deltaTime) {
        if (this.loopcount === 0) return;

        if (this.onBegin !== null && this.currentFrameIdx === 0) this.onBegin();
        if (this.onEnd !== null && this.currentFrameIdx === this.frames.length - 1) this.onEnd();

        const currentFrame = this.frames[this.currentFrameIdx];
        currentFrame.accumulator += deltaTime; 

        if (currentFrame.accumulator >= currentFrame.lifetime) {
            currentFrame.accumulator = 0;
            this.currentFrameIdx = (this.currentFrameIdx + 1) % this.frames.length;
            if (this.loopcount !== -1 && this.currentFrameIdx === this.frames.length - 1) this.loopcount--;  
        }
    }

    draw(canvas,ctx) {
        const currentFrame = this.frames[this.currentFrameIdx];
        currentFrame.draw(canvas,ctx);
    }

    static linearMotion(img,[startX, startY], [endX, endY], easing, fps = 60, loopcount = 1, [scaleX, scaleY] = [1,1],rotation = 0,onBegin = null,onEnd = null) {
        const dx = endX - startX;
        const dy = endY - startY;
        const distance = Math.sqrt(dx * dx + dy * dy);
        const [unitX, unitY] = [dx / distance, dy / distance];

        let frames = [];

        for (let i = 0; i < fps; i++) {
            const t = i / fps; 
            const current = [
                startX + unitX * distance * easing(t), 
                startY + unitY * distance * easing(t), 
            ];
            frames.push(new Frame(img, 1 / fps, current, rotation, [scaleX,scaleY]));
        }

        return new Animation(frames, loopcount, onBegin, onEnd);
    }
}

const loadImage = (url) => {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = (err) => reject(err);
    img.src = url;
  });
};

function cardToHash({symbol,suit}) {
    const formatted = suit.charAt(0).toUpperCase() + suit.slice(1).toLowerCase();
    return `card${formatted}${symbol}`
}

const cardsDirectory = 'Cards'
const chipsDirectory = 'Chips'

const backCard = 'cardBack_blue4';

const cardNames = [
    'cardClubs2',
    'cardClubs3',
    'cardClubs4',
    'cardClubs5',
    'cardClubs5',
    'cardClubs6',
    'cardClubs7',
    'cardClubs8',
    'cardClubs9',
    'cardClubs10',
    'cardClubsA',
    'cardClubsJ',
    'cardClubsQ',
    'cardClubsK',

    'cardHearts2',
    'cardHearts3',
    'cardHearts4',
    'cardHearts5',
    'cardHearts5',
    'cardHearts6',
    'cardHearts7',
    'cardHearts8',
    'cardHearts9',
    'cardHearts10',
    'cardHeartsA',
    'cardHeartsJ',
    'cardHeartsQ',
    'cardHeartsK',

    'cardSpades2',
    'cardSpades3',
    'cardSpades4',
    'cardSpades5',
    'cardSpades5',
    'cardSpades6',
    'cardSpades7',
    'cardSpades8',
    'cardSpades9',
    'cardSpades10',
    'cardSpadesA',
    'cardSpadesJ',
    'cardSpadesQ',
    'cardSpadesK',

    'cardDiamonds2',
    'cardDiamonds3',
    'cardDiamonds4',
    'cardDiamonds5',
    'cardDiamonds5',
    'cardDiamonds6',
    'cardDiamonds7',
    'cardDiamonds8',
    'cardDiamonds9',
    'cardDiamonds10',
    'cardDiamondsA',
    'cardDiamondsJ',
    'cardDiamondsQ',
    'cardDiamondsK',
]

const crossProd = ([x1,y1,z1], [x2,y2,z2]) => [
    y1*z2 - z1*y2,
    z1*x2 - x1*z2,
    x1*y2 - y1*x2
];

const normalize = ([x,y,z]) => {
    const length = Math.sqrt(x * x + y * y + z * z);
    return [x / length, y / length, z / length];
}

function Canvas({currGameState,prevGameState,clientHand}) {
    const canvasRef = useRef(null);
    const ctxRef = useRef(null);
    const assetMapRef = useRef(new Map());
    const [assetsLoaded,setAssetsLoaded] = useState(false);

    const [width, setWidth] = useState(null); 
    const [height, setHeight] = useState(null);

    const drawImage = (img, x, y, ndcWidth, ndcHeight, angle = 0) => {
        const ctx = ctxRef.current;
        const canvas = canvasRef.current;
        if (!ctx || !canvas) return;

        const width = canvas.width;
        const height = canvas.height;
        const aspectRatio = width / height;

        const screenX = (x + 1) * width / 2;
        const screenY = (y + 1) * height / 2;

        const pixelWidth  = (ndcWidth  / 2) * width  / aspectRatio;
        const pixelHeight = (ndcHeight / 2) * height;

        if (angle !== 0) {
            ctx.save();
            ctx.translate(screenX, screenY);
            ctx.rotate(angle * Math.PI / 180);
            ctx.drawImage(img, -pixelWidth / 2, -pixelHeight / 2, pixelWidth, pixelHeight);
            ctx.restore();
        } else {
            ctx.drawImage(img, screenX - pixelWidth / 2, screenY - pixelHeight / 2, pixelWidth, pixelHeight);
        }
    };

    const ndcToScreen = ([x,y]) => {
        return [(x + 1) * width / 2, (y + 1) * height / 2 ]; 
    }

    // expects normalized coordinates
    const positionsOnCircle = (numPlayers,[x,y] = [0,0],radius = 1.0) => {
        let result = [];
        let ndcResult = [];
        const canvas = canvasRef.current;
        const width = canvas.width;
        const height = canvas.height;  
        const aspectRatio = width / height;

        for(let i = 0; i < numPlayers; i++) {
            const [unitX,unitY] = [Math.cos(2 * Math.PI * i / numPlayers),Math.sin(2 * Math.PI * i / numPlayers)]
            let normPos = undefined; 
            if (aspectRatio > 1) {
                normPos = [ x - (radius * unitX) / aspectRatio, y - (radius * unitY) ];
            } else {
                normPos = [ x - (radius * unitX), y - (radius * unitY) * aspectRatio ];
            }
            ndcResult.push(normPos);
            const [normX,normY] = normPos;
            const screenSpacePos = [(normX + 1) * width / 2, (normY + 1) * height / 2 ];
            result.push(screenSpacePos);
        }
        return [result,ndcResult];
    }

    const inRowCardPositions = (numCards, [cx, cy], cardWidth) => {
        const canvas = canvasRef.current;
        const aspectRatio = canvas.width / canvas.height;
        
        const visualCardWidth = cardWidth / aspectRatio;
        const boundingBoxWidth = numCards * visualCardWidth;

        const result = [];
        for (let i = 0; i < numCards; i++) {
            const ndcX = cx - boundingBoxWidth / 2 + i * visualCardWidth + visualCardWidth / 2;
            const ndcY = cy;
            result.push([ndcX, ndcY]);
        }
        return result;
    };
    
    // init
    useEffect(() => {
        const canvas = document.getElementById('canvas');
        canvasRef.current = canvas;
        ctxRef.current = canvas.getContext('2d');
        
        const handleResize = () => {
            if (!canvasRef.current) return;
            const canvas = canvasRef.current;

            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;

            setWidth(canvas.width);
            setHeight(canvas.height);
        }

        handleResize();

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize',handleResize);
        }
    },[]);

    // asset loading
    useEffect(() => {
        const assetMap = assetMapRef.current;

        const loadAssets = async () => {
            const cardBackImg = await loadImage(`/${cardsDirectory}/${backCard}.png`);
            assetMap.set(backCard, cardBackImg); 
            for(const cardName of cardNames) {
                const img = await loadImage(`/${cardsDirectory}/${cardName}.png`);
                assetMap.set(cardName, img); 
            }

            for(const audioName of audioNames) {
                const audio = new Audio(`/${audioDirectory}/${audioName}.ogg`);
                assetMap.set(audioName, audio); 
            }

            setAssetsLoaded(true);
        }

        loadAssets();

        return () => {

        }
    },[]);
        

    const randomInsideCircle = ([x,y], radius) => {
        const angle = Math.random() * 180;
        const randRadius = Math.random() * radius;

        return [x - radius * Math.cos(angle), y - radius * Math.sin(angle)];
    }

    // rendering
    useEffect(() => {
        if (!ctxRef.current) return;
        if (!assetsLoaded) return;
        if (!currGameState) return;

        const shouldDealCards = (currGameState, prevGameState) => {
            return prevGameState.roundCount === 0 || currGameState.roundCount > prevGameState.roundCount;
        }

        const shouldDealFlop = (currGameState, prevGameState) => {
            for (let i = 0; i < 3; i++) {
                if (prevGameState.communityCards[i] !== null || currGameState.communityCards[i] === null) return false;
            }
            return true;
        }

        const shouldDealTurn = (currGameState, prevGameState) => {
            return prevGameState.communityCards[3] === null && currGameState.communityCards[3] !== null;
        }

        const shouldDealRiver = (currGameState, prevGameState) => {
            return prevGameState.communityCards[4] === null && currGameState.communityCards[4] !== null;
        }

        const shouldDealCommunity = (currGameState, prevGameState) => {
            for(let i = 0; i < 5; i++) {
                if (currGameState.communityCards[i] !== null && prevGameState.communityCards[i] === null) return true;
            }
            return false;
        }

        const clientName = usernameFromToken();

        const ctx = ctxRef.current;
        const canvas = canvasRef.current;

        ctx.clearRect(0, 0, width, height);

        const players = currGameState.players;
        const tableCenter = [0,0];
        const tableRadius = 0.8;

        const [positions,ndcSeatPositions] = positionsOnCircle(players.length,tableCenter,tableRadius);        
        const [cardPositions,ndcCardPositions] = positionsOnCircle(players.length,tableCenter,tableRadius - 0.05);        
        
        const assetMap = assetMapRef.current;
        const cardBackImg = assetMap.get(backCard);

        let animationId;
        let lastTime = performance.now();
        
        const easing = (t) => 1 - 1 * (1-t) * (1-t);
        const cardScale = [0.2,0.4];

        const playCardSlideSound = () => {
            const assetMap = assetMapRef.current;
            const index = Math.floor((Math.random() * 2 + 3));
            const sound = assetMap.get(audioNames[index]);
            sound.currentTime = 0;
            sound.play();
        }

        const dealCardsAnimations = [];
        for (let i = 0; i < players.length; i++) {
            const playerName = players[i].name;

            const [cardX,cardY] = ndcCardPositions[i];
            const cardToCenter = [-cardX, -cardY, 0.0];
            const tangentVec = normalize(crossProd(cardToCenter,[0,0,1]));
            let randomAngle = Math.random() * 30 - 15;
            const distanceOffset = 0.025;
            
            const styledCardPos = [
                cardX - (tangentVec[0] + cardToCenter[0]) * distanceOffset,
                cardY - (tangentVec[1] + cardToCenter[1]) * distanceOffset
            ]; 

            if (playerName === clientName) {
                const firstCard = clientHand.first;
                const firstImage = assetMap.get(cardToHash(firstCard));
                dealCardsAnimations.push(Animation.linearMotion(firstImage,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
            } else {
                dealCardsAnimations.push(Animation.linearMotion(cardBackImg,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
            }
        }

        for (let i = 0; i < players.length; i++) {
            const playerName = players[i].name;

            const [cardX,cardY] = ndcCardPositions[i];
            const cardToCenter = [-cardX, -cardY, 0.0];
            const tangentVec = normalize(crossProd(cardToCenter,[0,0,1]));
            let randomAngle = Math.random() * 30 - 15;
            const distanceOffset = 0.025;
            
            const styledCardPos = [
                cardX + (tangentVec[0] + cardToCenter[0]) * distanceOffset,
                cardY + (tangentVec[1] + cardToCenter[1]) * distanceOffset
            ]; 

            if (playerName === clientName) {
                const secondCard = clientHand.second;
                const secondImage = assetMap.get(cardToHash(secondCard));
                dealCardsAnimations.push(Animation.linearMotion(secondImage,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
            } else {
                dealCardsAnimations.push(Animation.linearMotion(cardBackImg,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
            }
        }

        const communityCardsAnimations = [];

        const communityCards = currGameState.communityCards;
        const communityCardsPos = inRowCardPositions(5,[0.0,0],0.25);
        for(let i = 0; i < 5; i++) {
            const communityCard = communityCards[i];
            if (communityCard === null) continue;
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            communityCardsAnimations.push(Animation.linearMotion(image,tableCenter,communityCardsPos[i],easing,60,1,cardScale,randomAngle,playCardSlideSound));  
        }

        let dealt = 0;
        let communityDealt = 0;

        function loop(timestamp) {
            const delta = (timestamp - lastTime) / 1000;
            lastTime = timestamp;

            ctx.clearRect(0, 0, width, height);

            if (shouldDealCards(currGameState, prevGameState) && dealt < dealCardsAnimations.length) {
                dealCardsAnimations[dealt].update(delta);
                dealCardsAnimations[dealt].draw(canvas,ctx);

                if (dealCardsAnimations[dealt].finished()) {
                    dealt++; 
                }
            }

            for (let i = dealt - 1; i >= 0; i--) {
                if (dealCardsAnimations[i].finished())
                    dealCardsAnimations[i].draw(canvas,ctx);
            }

            if (shouldDealCommunity(currGameState, prevGameState) && communityDealt < communityCardsAnimations.length) {
                communityCardsAnimations[communityDealt].update(delta);
                communityCardsAnimations[communityDealt].draw(canvas,ctx);

                if (communityCardsAnimations[communityDealt].finished()) {
                    communityDealt++; 
                }
            }

            for (let i = communityDealt - 1; i >= 0; i--) {
                if (communityCardsAnimations[i].finished())
                    communityCardsAnimations[i].draw(canvas,ctx);
            }

            const fontSize = height * 0.025;
            for(let i = 0; i < positions.length; i++) {
                const [x,y] = positions[i];
                const playerName = players[i].name;
                const metrics = ctx.measureText(playerName);
                const textWidth = metrics.width;
                const textHeight = fontSize;
                ctx.font = `${fontSize}px Arial`;
                ctx.fillStyle = "black";
                ctx.fillText(playerName, x - textWidth / 2, y - textHeight / 2);
            }


            animationId = requestAnimationFrame(loop);
        }

        animationId = requestAnimationFrame(loop);


        return () => cancelAnimationFrame(animationId);
    },[assetsLoaded,ctxRef,currGameState])

    return (
        <canvas id="canvas" ref={canvasRef} width={window.innerWidth} height={window.innerHeight} style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            zIndex: 0,
        }} />
    );
}

export default Canvas;
