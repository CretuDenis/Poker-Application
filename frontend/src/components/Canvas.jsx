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

function drawText(canvas,ctx,str,color,[x,y],fontSize,fontName = 'Arial') {
    ctx.save();

    const metrics = ctx.measureText(str);
    const textWidth = metrics.width;
    const textHeight = fontSize;

    const screenX = (x + 1) * canvas.width / 2;
    const screenY = (y + 1) * canvas.height / 2;

    ctx.font = `${fontSize}px ${fontName}`;
    ctx.fillStyle = color;

    ctx.translate(screenX, screenY);
    ctx.fillText(str, -textWidth / 2, -textHeight / 2);

    ctx.restore();
}

class Animation {
    constructor(frames, loopcount = 1, onBegin = null, onEnd = null) {
        this.frames = frames;
        this.currentFrameIdx = 0;
        this.loopcount = loopcount;
        this.onBegin = onBegin;
        this.onEnd = onEnd;
        this.dependencies = [];
        this.keepOnFinish = false;
    }


    finished() {
        return this.currentFrameIdx === this.frames.length - 1;
    }

    addDependency(other) {
        this.dependencies.push(other);
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
        if (distance === 0) {
            return new Animation([new Frame(img, 1 / fps, [startX,startY], rotation, [scaleX,scaleY])], loopcount, onBegin, onEnd);
        }
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

const chipsNames = [
    'chipBlackWhite',
    'chipBlueWhite',
    'chipGreenWhite',
    'chipRedWhite',
    'chipWhiteBlue',
];

const chipValues = [
    100,
    50,
    25,
    10,
    1
];

const crossProd = ([x1,y1,z1], [x2,y2,z2]) => [
    y1*z2 - z1*y2,
    z1*x2 - x1*z2,
    x1*y2 - y1*x2
];

const normalize = ([x,y,z]) => {
    const length = Math.sqrt(x * x + y * y + z * z);
    return [x / length, y / length, z / length];
}

const calculateAnimations = (curr,prev) => {
    const animations = [];    
    


    return animations;
}

function Canvas({currGameState,prevGameState,clientHand }) {
    const canvasRef = useRef(null);
    const ctxRef = useRef(null);
    const assetMapRef = useRef(new Map());

    const dealCardsAnimationsRef = useRef([]);
    const communityCardsAnimationsRef = useRef([]);
    const dealtRef = useRef(0);
    const dealtCommunityRef = useRef(0);
    
    const [width, setWidth] = useState(null); 
    const [height, setHeight] = useState(null);
    const [assetsLoaded,setAssetsLoaded] = useState(false);

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
        return ndcResult;
    }

    const sequentialRects = (numRects, [x, y], rectsWidth,padding = 0,rotation = 0) => {
        const boundingBoxWidth = (numRects + 1) * padding + numRects * rectsWidth;
        
        const result = [];
        const radians = rotation * Math.PI / 180;
        const sin = Math.sin(radians);
        const cos = Math.cos(radians);

        for (let i = 0; i < numRects; i++) {
            const [posX, posY]= [x - boundingBoxWidth / 2 + padding + rectsWidth / 2 + i * (padding + rectsWidth), y];
            const [tempX, tempY] = [posX - x, posY - y];

            const [rotatedX, rotateY] = [
                tempX * cos - tempY * sin,
                tempX * sin + tempY * cos,
            ];

            result.push([rotatedX + x, rotateY + y]);
        }
        console.log(result);
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
    },[]);
        
    // rendering
    useEffect(() => {
        if (!ctxRef.current) return;
        if (!assetsLoaded) return;
        if (!currGameState) return;
        if (!currGameState) return;

        const clientName = usernameFromToken();

        const ctx = ctxRef.current;
        const canvas = canvasRef.current;

        ctx.clearRect(0, 0, width, height);

        const players = currGameState.players;
        const tableCenter = [0,0];
        const tableRadius = 0.8;

        const ndcCardPositions = positionsOnCircle(players.length,tableCenter,tableRadius + 0.1);        
        const namesPos = positionsOnCircle(players.length,tableCenter,tableRadius - 0.15);
        
        const assetMap = assetMapRef.current;
        const cardBackImg = assetMap.get(backCard);

        let animationId;
        let lastTime = performance.now();
        
        const easing = (t) => 1 - 1 * (1-t) * (1-t);
        const cardScale = [0.15,0.30];

        const shouldDealFlop = (curr,prev) => {
            return prev !== null && curr.communityCards[0] !== null && prev.communityCards[0] === null;
        }
        const shouldDealTurn = (curr,prev) => {
            return prev !== null && curr.communityCards[3] !== null && prev.communityCards[3] === null;
        }
        const shouldDealRiver = (curr,prev) => {
            return prev !== null && curr.communityCards[4] !== null && prev.communityCards[4] === null;
        }

        const shouldDealPlayerCards = (curr,prev) => {
            return prev === null || (prev.round != curr.round)
        }

        const playCardSlideSound = () => {
            const assetMap = assetMapRef.current;
            const index = Math.floor((Math.random() * 2 + 3));
            const sound = assetMap.get(audioNames[index]);
            sound.currentTime = 0;
            sound.play();
        }

        if (shouldDealPlayerCards(currGameState,prevGameState)) {
            dealtRef.current = 0;
            dealCardsAnimationsRef.current = [];
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
                    dealCardsAnimationsRef.current.push(Animation.linearMotion(firstImage,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
                } else {
                    dealCardsAnimationsRef.current.push(Animation.linearMotion(cardBackImg,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
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
                    dealCardsAnimationsRef.current.push(Animation.linearMotion(secondImage,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
                } else {
                    dealCardsAnimationsRef.current.push(Animation.linearMotion(cardBackImg,tableCenter,styledCardPos,easing,60,1,cardScale,randomAngle,playCardSlideSound));  
                }
            }
        } else if (dealCardsAnimationsRef.current.length === 0 && prevGameState !== null && prevGameState.round === currGameState.round) {
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
                    const anim = new Animation([new Frame(firstImage, 0, styledCardPos, randomAngle,cardScale)]);
                    dealCardsAnimationsRef.current.push(anim);  
                } else {
                    const anim = new Animation([new Frame(cardBackImg, 0, styledCardPos, randomAngle,cardScale)]);
                    dealCardsAnimationsRef.current.push(anim);  
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
                    const anim = new Animation([new Frame(secondImage, 0, styledCardPos, randomAngle,cardScale)]);
                    dealCardsAnimationsRef.current.push(anim);  
                } else {
                    const anim = new Animation([new Frame(cardBackImg, 0, styledCardPos, randomAngle,cardScale)]);
                    dealCardsAnimationsRef.current.push(anim);  
                }
            }

        }
        
        if (prevGameState !== null && currGameState.round > prevGameState.round)  {
            dealtCommunityRef.current = 0;
            communityCardsAnimationsRef.current = [];
        }
        
        const communityCards = currGameState.communityCards;
        const communityCardsPos = sequentialRects(5,[0.2,0],cardScale[0],30);

        if (shouldDealFlop(currGameState,prevGameState)) {
            for(let i = 0; i < 3; i++) {
                const communityCard = communityCards[i];
                const image = assetMap.get(cardToHash(communityCard));
                const randomAngle = Math.random() * 10 - 5;
                communityCardsAnimationsRef.current.push(Animation.linearMotion(image,tableCenter,communityCardsPos[i],easing,60,1,cardScale,randomAngle,playCardSlideSound));  
            }
        } else if (communityCardsAnimationsRef.current.length === 0 && communityCards[0] !== null) {
            for(let i = 0; i < 3; i++) {
                const communityCard = communityCards[i];
                const image = assetMap.get(cardToHash(communityCard));
                const randomAngle = Math.random() * 10 - 5;
                const anim = new Animation([new Frame(image, 0, communityCardsPos[i],randomAngle,cardScale)]);
                communityCardsAnimationsRef.current.push(anim);  
            }
        }
        
        if (shouldDealTurn(currGameState, prevGameState)) {
            const communityCard = communityCards[3];
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            communityCardsAnimationsRef.current.push(Animation.linearMotion(image,tableCenter,communityCardsPos[3],easing,60,1,cardScale,randomAngle,playCardSlideSound));  
        } else if (communityCardsAnimationsRef.current.length === 3 && communityCards[3] !== null) {
            const communityCard = communityCards[3];
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            const anim = new Animation([new Frame(image, 0, communityCardsPos[3],randomAngle,cardScale)]);
            communityCardsAnimationsRef.current.push(anim);  
        }
        if (shouldDealRiver(currGameState, prevGameState)) {
            const communityCard = communityCards[4];
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            communityCardsAnimationsRef.current.push(Animation.linearMotion(image,tableCenter,communityCardsPos[4],easing,60,1,cardScale,randomAngle,playCardSlideSound));  
        } else if (communityCardsAnimationsRef.current.length === 4 && communityCards[4] !== null) {
            const communityCard = communityCards[4];
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            const anim = new Animation([new Frame(image, 0, communityCardsPos[4],randomAngle,cardScale)]);
            communityCardsAnimationsRef.current.push(anim);  
        }

        function loop(timestamp) {
            const delta = (timestamp - lastTime) / 1000;
            lastTime = timestamp;

            ctx.clearRect(0, 0, width, height);
            if (dealtRef.current < dealCardsAnimationsRef.current.length) {
                dealCardsAnimationsRef.current[dealtRef.current].update(delta);
                dealCardsAnimationsRef.current[dealtRef.current].draw(canvas,ctx);

                if (dealCardsAnimationsRef.current[dealtRef.current].finished()) {
                    dealtRef.current++; 
                }
            }

            for (const animation of dealCardsAnimationsRef.current) {
                if (animation.finished())
                    animation.draw(canvas,ctx);
            }

            if (dealtCommunityRef.current < communityCardsAnimationsRef.current.length) {
                communityCardsAnimationsRef.current[dealtCommunityRef.current].update(delta);
                communityCardsAnimationsRef.current[dealtCommunityRef.current].draw(canvas,ctx);

                if (communityCardsAnimationsRef.current[dealtCommunityRef.current].finished()) {
                    dealtCommunityRef.current++; 
                }
            }

            for (const animation of communityCardsAnimationsRef.current) {
                if (animation.finished())
                    animation.draw(canvas,ctx);
            }

            for(let i = 0; i < namesPos.length; i++) {
                const playerName = players[i].name;
                const color = playerName === currGameState.speaking ? 'red' : 'black';
                drawText(canvas,ctx,playerName,color,namesPos[i],20);
            }

            animationId = requestAnimationFrame(loop);
        }
        //animationId = requestAnimationFrame(loop);
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
