import {useState, useEffect, useRef } from "react"
import { usernameFromToken } from '../api'

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

function Canvas({currGameState,clientHand},prevGameState) {
    const canvasRef = useRef(null);
    const animationFrameRef = useRef(0);
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
        const ctx = ctxRef.current;
        ctx.clearRect(0, 0, width, height);

        const players = currGameState.players;
        const tableCenter = [0,0];
        const tableRadius = 0.75;
        const [positions,ndcPositins] = positionsOnCircle(players.length,tableCenter,tableRadius);        
        const [cardPositions,ndcCardPositions] = positionsOnCircle(players.length,tableCenter,tableRadius - 0.20);        
        
        const assetMap = assetMapRef.current;
        const cardBackImg = assetMap.get(backCard);

        let animationId;
        let lastTime = 0;

        function loop(timestamp) {
            const delta = (timestamp - lastTime) / 1000;
            lastTime = timestamp;

                

            animationId = requestAnimationFrame(loop);
        }

        //animationId = requestAnimationFrame(loop);
        

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
        
        const cardDistanceThreshold = 0.1;
        const clientName = usernameFromToken();
        const backOfCard = assetMap.get(backCard);
        for(let i = 0; i < positions.length; i++) {
            const playerName = players[i].name;
            const [cardX,cardY] = ndcCardPositions[i];

            const cardToCenter = [-cardX, -cardY, 0.0];
            console.log(cardToCenter);
            const tangentVec = normalize(crossProd(cardToCenter,[0,0,1]));
            
            let randomAngle = Math.random() * 40 - 20;
            const distanceOffset = 0.025;

            if (playerName === clientName) {
                const firstCard = clientHand.first;
                const secondCard = clientHand.second;
                if (!firstCard || !secondCard) continue;
                
                const firstImage = assetMap.get(cardToHash(firstCard));
                const secondImage = assetMap.get(cardToHash(secondCard));
                drawImage(firstImage, cardX + (tangentVec[0] + cardToCenter[0]) * distanceOffset,cardY + (tangentVec[1] + cardToCenter[1]) * distanceOffset, 0.15,0.20,randomAngle);
                randomAngle = Math.random() * 40 - 20;
                drawImage(secondImage, cardX - (tangentVec[0] + cardToCenter[0]) * distanceOffset,cardY - (tangentVec[1] + cardToCenter[1]) * distanceOffset, 0.15,0.20,randomAngle);
            } else {
                drawImage(backOfCard, cardX + (tangentVec[0] + cardToCenter[0]) * distanceOffset,cardY + (tangentVec[1] + cardToCenter[1]) * distanceOffset, 0.15,0.20,randomAngle);
                randomAngle = Math.random() * 40 - 20;
                drawImage(backOfCard, cardX - (tangentVec[0] + cardToCenter[0]) * distanceOffset,cardY - (tangentVec[1] + cardToCenter[1]) * distanceOffset, 0.15,0.20,randomAngle);
            }
        }

        const communityCards = currGameState.communityCards;
        const communityCardsPos = inRowCardPositions(5,[0.0,0],0.15);
        for(let i = 0; i < 5; i++) {
            const communityCard = communityCards[i];
            if (communityCard === null) continue;
            const [x,y] = communityCardsPos[i];
            const image = assetMap.get(cardToHash(communityCard));
            const randomAngle = Math.random() * 10 - 5;
            drawImage(image,x,y,0.15,0.20,randomAngle);
        }
    },[assetsLoaded,width,height,ctxRef,currGameState])

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
