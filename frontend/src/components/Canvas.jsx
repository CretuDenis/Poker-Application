import {useState, useEffect, useRef } from "react"
import { usernameFromToken } from '../api'

function randomInRange(min,max) {
    const array = new Uint32Array(1);
    window.crypto.getRandomValues(array);
    const random = array[0] / (0xFFFFFFFF + 1);
    return random * (max - min) + min;
}

const animationFPS = 30;

const fontDirectory = 'fonts'
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

const fontNames = [
    'JqkasWild-w1YD6'
]

const textureNames = [
    'table_texture',
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

function drawText(canvas, ctx, str, [r, g, b], [x, y], fontSize, fontName = 'Arial') {
    ctx.save();

    ctx.font = `${fontSize}px ${fontName}`;

    const metrics = ctx.measureText(str);
    const textWidth = metrics.width;
    const textHeight = fontSize;

    const screenX = (x + 1) * canvas.width / 2;
    const screenY = (y + 1) * canvas.height / 2;

    ctx.translate(screenX, screenY);

    ctx.strokeStyle = `rgb(${255 - r}, ${255 - g}, ${255 - b})`;
    ctx.lineWidth = 4;
    ctx.lineJoin = 'round';

    ctx.strokeText(str, -textWidth / 2, -textHeight / 2);
    ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
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
        this.label = null;
    }

    setLabel(name) {
        this.label = name;
    }
    
    canUpdate() {
        for (const dependency of this.dependencies) {
            if (!dependency.finished()) return false;
        }
        return true;
    }

    canDraw() {
        for (const dependency of this.dependencies) {
            if (!dependency.finished()) return false;
        }
        return true;
    }

    finished() {
        return this.currentFrameIdx === this.frames.length - 1 && this.loopcount === 0;
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

const chipsValues = [
    100,
    50,
    25,
    10,
    1
];

const chipsValuesDict = {
    100: 'chipBlackWhite',
    50: 'chipBlueWhite',
    25: 'chipGreenWhite',
    10: 'chipRedWhite',
    1: 'chipWhiteBlue',
}

const crossProd = ([x1,y1,z1], [x2,y2,z2]) => [
    y1*z2 - z1*y2,
    z1*x2 - x1*z2,
    x1*y2 - y1*x2
];

const normalize = ([x,y,z]) => {
    const length = Math.sqrt(x * x + y * y + z * z);
    return [x / length, y / length, z / length];
}


function Canvas({ currGameState,prevGameState }) {
    const canvasRef = useRef(null);
    const ctxRef = useRef(null);
    const assetMapRef = useRef(new Map());
    const gameOverRef = useRef(false);

    const animationsRef = useRef([]);
    
    const [width, setWidth] = useState(null); 
    const [height, setHeight] = useState(null);
    const [assetsLoaded,setAssetsLoaded] = useState(false);

    const normalizeAngle = (angle) => {
        return angle - Math.floor(angle / 360) * 360;
    }

    const pointsOnSquare = (numPoints,length = 1) => {
        let result = [];
        const degreeInc = 360 / numPoints;
        let currentAngle = 270;

        for(let i = 0; i < numPoints; i++) {
            currentAngle = normalizeAngle(currentAngle);
            const currentRadians = currentAngle * Math.PI / 180;
            const sin = -Math.sin(currentRadians);
            const cos = Math.cos(currentRadians);

            let pos;
            if (currentAngle >= 45 && currentAngle < 135) {
                pos = [cos, -length]; 
            } else if (currentAngle >= 135 && currentAngle < 225) {
                pos = [-length, sin]; 
            } else if (currentAngle >= 225 && currentAngle < 315) {
                pos = [cos, length]; 
            } else {
                pos = [length, sin]; 
            }
            result.push(pos);
            currentAngle -= degreeInc;
        }
        return result;
    }

    const getPlayersInOrder = () => {
        const clientName = usernameFromToken();
        const players = currGameState.players;
        const result = [];
        let i = 0;
        while (players[i].name !== clientName)
            i++;
        result.push(players[i]); 

        while (result.length !== players.length) {
            i = (i + 1) % players.length;
            result.push(players[i]);
        } 

        return result;
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
        return result;
    };

    useEffect(() => {
        if (gameOverRef.current) 
            navigate('/');
    },[gameOverRef])
    
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

            for(const chipsName of chipsNames) {
                const img = await loadImage(`/${chipsDirectory}/${chipsName}.png`);
                assetMap.set(chipsName, img); 
            }

            for(const fontName of fontNames) {
                const font = new FontFace(fontName,`url(/${fontDirectory}/${fontName}.ttf)`);

                const loadedFont = await font.load();
                if (!loadedFont) {
                    console.log(`Failed to load font: ${fontName}`);
                    continue;
                }
                document.fonts.add(loadedFont);
            }

            for(const textureName of textureNames) {
                const img = await loadImage(`/assets/${textureName}.jpg`);
                assetMap.set(textureName, img); 
            }

            setAssetsLoaded(true);
        }

        loadAssets();
    },[]);
        
    // rendering
    useEffect(() => {
        if (!ctxRef.current) return;
        if (!assetsLoaded) return;
        if (currGameState === null) {
            navigate('/');
            return;
        }

        const clientName = usernameFromToken();

        let clientFound = false;
        for(const player of currGameState.players) {
            if (player.name === clientName) {
                clientFound = true;
                break;
            }
        }
        
        if (!clientFound) {
            gameOverRef.current = true;
            return;
        }

        const ctx = ctxRef.current;
        const canvas = canvasRef.current;

        ctx.clearRect(0, 0, width, height);

        const players = getPlayersInOrder();
        const tableCenter = [0,0];

        const ndcCardPositions = pointsOnSquare(players.length,0.9);
        const namesPos = pointsOnSquare(players.length,0.70);
        
        const assetMap = assetMapRef.current;
        const cardBackImg = assetMap.get(backCard);

        let animationId;
        let lastTime = performance.now();
        
        const easing = (t) => 1 - 1 * (1-t) * (1-t);
        const cardScale = [0.25,0.40];

        const playCardSlideSound = () => {
            const assetMap = assetMapRef.current;
            const index = Math.floor((Math.random() * 2 + 3));
            const sound = assetMap.get(audioNames[index]);
            sound.currentTime = 0;
            sound.play();
        }

        const playChipsCollide = () => {
            const assetMap = assetMapRef.current;
            const index = Math.floor((Math.random() * 1  + 2));
            const sound = assetMap.get(`chipsCollide${index}`);
            sound.currentTime = 0;
            sound.play();
        }

        const labelPresentIn = (animationArr,label) => {
            for(const animation of animationArr) {
                if (label == animation.label) return true;
            }
            return false;
        }
        
        const angleBetween = ([x1,y1],[x2,y2]) => {
            const l1 = Math.sqrt(x1 * x1 + y1 * y1);
            const l2 = Math.sqrt(x2 * x2 + y2 * y2);
            const cos = (x1 * x2 + y1 * y2) / (l1 * l2); 
            return Math.acos(cos) * 180 / Math.PI;
        }

        let disconnectedPlayersChips = [];

        const generateAnimations = () => {
            animationsRef.current = [];

            const roundPassedOrFirstRound = prevGameState === null || (prevGameState.round != currGameState.round);
            const shouldDrawStaticPlayerCards = animationsRef.current.length === 0 && prevGameState !== null && prevGameState.round === currGameState.round;

            const sideVectors = [];

            for (let i = 0; i < 2 * players.length; i++) {
                const playerIndex = i % players.length;
                const passCount = Math.floor(i / players.length);

                const [cardX,cardY] = ndcCardPositions[playerIndex];
                const cardToCenter = [-cardX, -cardY, 0.0];
                const tangentVec = normalize(crossProd(cardToCenter,[0,0,1]));
                sideVectors.push(tangentVec);
                const defaultAngle = Math.sign(cardX) * angleBetween([0,Math.sign(-cardY)], cardToCenter);
                const randomAngle = randomInRange(0,15) * (passCount === 0 ? 1 : -1);

                const angle = defaultAngle + randomAngle;
                const distanceOffset = (passCount === 0 ? -1 : 1) * 0.055;
                
                const styledCardPos = [
                    cardX + (tangentVec[0] + cardToCenter[0]) * distanceOffset,
                    cardY + (tangentVec[1] + cardToCenter[1]) * distanceOffset
                ]; 

                let animation = undefined;
                if (players[playerIndex].hand === null) continue;
                const card = passCount === 0 ? players[playerIndex].hand.first : players[playerIndex].hand.second;
                const cardImage = card !== null ? assetMap.get(cardToHash(card)) : cardBackImg; 
                
                if (roundPassedOrFirstRound)
                    animation = Animation.linearMotion(cardImage,tableCenter,styledCardPos,easing,animationFPS,1,cardScale,angle,playCardSlideSound);
                else if (shouldDrawStaticPlayerCards)
                    animation = new Animation([new Frame(cardImage, 0, styledCardPos,angle,cardScale)]);

                if (animation) {
                    if (i !== 0 && !shouldDrawStaticPlayerCards) animation.addDependency(animationsRef.current[i-1]);
                    animationsRef.current.push(animation);
                }
            }

            const communityCards = currGameState.communityCards;
            const communityCardsPos = sequentialRects(5,[0.2,0],cardScale[0]);

            const shouldDealFlop = prevGameState !== null && currGameState.communityCards[0] !== null && prevGameState.communityCards[0] === null;
            const shouldDrawStaticFlop = !labelPresentIn(animationsRef.current,'flop') && communityCards[0] !== null;

            for(let i = 0; i < 3 && (shouldDealFlop || shouldDrawStaticFlop); i++) {
                const communityCard = communityCards[i];
                const flopImage = assetMap.get(cardToHash(communityCard));
                const randomAngle = Math.random() * 10 - 5;

                let animation = undefined;
                if (shouldDealFlop) {
                    animation = Animation.linearMotion(flopImage,tableCenter,communityCardsPos[i],easing,animationFPS,1,cardScale,randomAngle,playCardSlideSound);
                    animation.setLabel('flop');
                } else if (shouldDrawStaticFlop) {
                    animation = new Animation([new Frame(flopImage, 0, communityCardsPos[i],randomAngle,cardScale)]);
                    animation.setLabel('flop');
                }

                if (animation) {
                    if (i !== 0) {
                        const lastIndex = animationsRef.current.length - 1;
                        animation.addDependency(animationsRef.current[lastIndex]);
                    } 
                    animationsRef.current.push(animation);  
                }
            }

            const shouldDealTurn = prevGameState !== null && currGameState.communityCards[3] !== null && prevGameState.communityCards[3] === null;
            const shouldDrawStaticTurn = !labelPresentIn(animationsRef.current,'turn') && communityCards[3] !== null;

            if (shouldDealTurn || shouldDrawStaticTurn) {
                const turnCard = communityCards[3];
                const turnImage = assetMap.get(cardToHash(turnCard));
                const turnRandomAngle = Math.random() * 10 - 5;

                let turnAnimation = undefined;
                if (shouldDealTurn) {
                    turnAnimation = Animation.linearMotion(turnImage,tableCenter,communityCardsPos[3],easing,animationFPS,1,cardScale,turnRandomAngle,playCardSlideSound);
                    turnAnimation.setLabel('turn');
                } else if (shouldDrawStaticTurn) {
                    turnAnimation = new Animation([new Frame(turnImage, 0, communityCardsPos[3],turnRandomAngle,cardScale)]);
                    turnAnimation.setLabel('turn');
                }

                if (turnAnimation)
                    animationsRef.current.push(turnAnimation);  
            }

            const shouldDealRiver = prevGameState !== null && currGameState.communityCards[4] !== null && prevGameState.communityCards[4] === null;
            const shouldDrawStaticRiver = !labelPresentIn(animationsRef.current,'river') && communityCards[4] !== null;

            if (shouldDealRiver || shouldDrawStaticRiver) {
                const riverCard = communityCards[4];
                const riverImage = assetMap.get(cardToHash(riverCard));
                const riverRandomAngle = Math.random() * 10 - 5;

                let riverAnimation = undefined;
                if (shouldDealRiver) {
                    riverAnimation = Animation.linearMotion(riverImage,tableCenter,communityCardsPos[4],easing,animationFPS,1,cardScale,riverRandomAngle,playCardSlideSound);
                    riverAnimation.setLabel('river');
                } else if (shouldDrawStaticRiver) {
                    riverAnimation = new Animation([new Frame(riverImage, 0, communityCardsPos[4],riverRandomAngle,cardScale)]);
                    riverAnimation.setLabel('river');
                }

                if (riverAnimation)
                    animationsRef.current.push(riverAnimation);  
            }

            const getChipsTypesCount = (playerBet) => {
                const chipsFreq = new Map();
                for(const value of chipsValues)
                    chipsFreq.set(value,0);

                let bet = structuredClone(playerBet); 
                let i = 0;
                while(bet > 0 && i < chipsValues.length) {
                    const currChipValue = chipsValues[i];
                    const numChips = Math.floor(playerBet / currChipValue); 

                    if (numChips === 0) {
                        i++;
                        continue;
                    }

                    const currCount = chipsFreq.get(currChipValue) ?? 0;
                    chipsFreq.set(currChipValue,currCount + numChips);

                    if (playerBet % currChipValue === 0) break; 
                    playerBet -= Math.floor(numChips * currChipValue);
                }
                
                return chipsFreq;
            }

            const chipScale = [0.1,0.1];

            const playerExists = (arr,playerName) => {
                for (const player of arr) {
                    if (player.name === playerName) return true;
                }
                return false;
            }

            chipsValues.sort((a, b) => b - a); 
            for (let i = 0; i < players.length; i++) {
                const playerBet = players[i].bet;
                const playerBalance = players[i].balance;
                let prevPlayerBet = 0;
                if (prevGameState !== null) {
                    for(const prevPlayer of prevGameState.players) {
                        if (prevPlayer.name === players[i].name) {
                            prevPlayerBet = prevPlayer.bet;
                            break;
                        }
                    }
                }
                const toBet = playerBet - prevPlayerBet;
                
                const toAddInPotChipsFreq = getChipsTypesCount(toBet);
                const alreadyInPot = getChipsTypesCount(prevPlayerBet);
                const balanceChips = getChipsTypesCount(playerBalance);

                const chipsBasePos = [ ndcCardPositions[i][0] / 2, ndcCardPositions[i][1] / 2 ];
                const balanceChipsPos = [ ndcCardPositions[i][0], ndcCardPositions[i][1] ];
                const maxBetRadius = 0.10;

                for (const [chipValue, chipCount] of toAddInPotChipsFreq) {
                    if (chipCount === 0) continue;

                    for (let j = 0; j < chipCount; j++) {
                        const randomRadius = randomInRange(0,maxBetRadius); 
                        const randomAngle = randomInRange(0,360); 
                        const radians = randomAngle * Math.PI / 180;
                        const sin = Math.sin(radians);
                        const cos = Math.cos(radians);

                        const styledChipPos = [
                            chipsBasePos[0] + randomRadius * cos,
                            chipsBasePos[1] + randomRadius * sin
                        ];

                        const chipImage = assetMap.get(chipsValuesDict[chipValue]);
                        const animation = Animation.linearMotion(chipImage,ndcCardPositions[i],styledChipPos,easing,animationFPS,1,chipScale,0,playChipsCollide);
                        animation.setLabel('bet');
                        animationsRef.current.push(animation);
                    }
                }

                for (const [chipValue, chipCount] of alreadyInPot) {
                    if (chipCount === 0) continue;

                    for (let j = 0; j < chipCount; j++) {
                        const randomRadius = randomInRange(0,maxBetRadius); 
                        const randomAngle = randomInRange(0,360); 
                        const radians = randomAngle * Math.PI / 180;
                        const sin = Math.sin(radians);
                        const cos = Math.cos(radians);

                        const styledChipPos = [
                            chipsBasePos[0] + randomRadius * cos,
                            chipsBasePos[1] + randomRadius * sin
                        ];

                        const chipImage = assetMap.get(chipsValuesDict[chipValue]);
                        const animation = new Animation([new Frame(chipImage, 0,styledChipPos,0,chipScale)]);
                        animationsRef.current.push(animation);
                    }
                }

                const maxBalanceRadius = 0.10;
                for (const [chipValue, chipCount] of balanceChips) {
                    if (chipCount === 0) continue;

                    for (let j = 0; j < chipCount; j++) {
                        const randomRadius = randomInRange(0,maxBalanceRadius); 
                        const randomAngle = randomInRange(0,360); 
                        const radians = randomAngle * Math.PI / 180;
                        const sin = Math.sin(radians);
                        const cos = Math.cos(radians);
                        
                        const balancePosDistance = 0.3;

                        const styledChipPos = [
                            balanceChipsPos[0] + balancePosDistance * sideVectors[i][0] + randomRadius * cos,
                            balanceChipsPos[1] + balancePosDistance * sideVectors[i][1] + randomRadius * sin, 
                        ];

                        const chipImage = assetMap.get(chipsValuesDict[chipValue]);
                        const animation = new Animation([new Frame(chipImage, 0,styledChipPos,0,chipScale)]);
                        animationsRef.current.push(animation);
                    }
                }
            }

            if (prevGameState) {
                disconnectedPlayersChips = [];
                const prevPlayers = prevGameState.players;
                const prevNdcCardPositions = pointsOnSquare(prevPlayers.length,0.9);

                for (let i = 0; i < prevPlayers.length; i++) {
                    if (playerExists(players, prevPlayers[i].name)) continue; 
                    if (prevPlayers[i].bet === 0) continue; 

                    const disconnectedChips = getChipsTypesCount(prevPlayers[i].bet);

                    const chipsBasePos = [ prevNdcCardPositions[i][0] / 2, prevNdcCardPositions[i][1] / 2 ];
                    const maxBetRadius = 0.10;

                    disconnectedPlayersChips.push({
                        value: prevPlayers[i].bet,
                        pos: chipsBasePos,
                    });

                    for (const [chipValue, chipCount] of disconnectedChips) {
                        if (chipCount === 0) continue;

                        for (let j = 0; j < chipCount; j++) {
                            const randomRadius = randomInRange(0,maxBetRadius); 
                            const randomAngle = randomInRange(0,360); 
                            const radians = randomAngle * Math.PI / 180;
                            const sin = Math.sin(radians);
                            const cos = Math.cos(radians);

                            const styledChipPos = [
                                chipsBasePos[0] + randomRadius * cos,
                                chipsBasePos[1] + randomRadius * sin
                            ];

                            const chipImage = assetMap.get(chipsValuesDict[chipValue]);
                            const animation = new Animation([new Frame(chipImage, 0,styledChipPos,0,chipScale)]);
                            animationsRef.current.push(animation);
                        }
                    }
                }
            }
        }
        
        generateAnimations();
        
        ctx.clearRect(0, 0, width, height);
        function loop(timestamp) {
            const delta = (timestamp - lastTime) / 1000;
            lastTime = timestamp;

            ctx.clearRect(0, 0, width, height);
            const backgrounImg = assetMap.get('table_texture');
            ctx.drawImage(backgrounImg, 0, 0, canvas.width, canvas.height);

            for (const animation of animationsRef.current) {
                if (animation.canUpdate()) {
                    animation.update(delta);
                }
            } 

            for (const animation of animationsRef.current) {
                if (animation.canDraw()) {
                    animation.draw(canvas,ctx);
                }
            }

            for(let i = 0; i < namesPos.length; i++) {
                const balanceOffset = 0.075;
                const balancePos = [namesPos[i][0],namesPos[i][1] + balanceOffset ];
                const playerName = players[i].name;
                const color = playerName === currGameState.speaking ? [255,0,0] : [255,255,255];
                drawText(canvas,ctx,playerName,color,namesPos[i],30,'JqkasWild-w1YD6');
                drawText(canvas,ctx,`Balance: ${players[i].balance}$`,color,balancePos,30,'JqkasWild-w1YD6');
            }

            for (let i = 0; i < players.length; i++) {
                if (players[i].bet === 0) continue;
                const betPos = [ndcCardPositions[i][0] / 2, ndcCardPositions[i][1] / 2];
                drawText(canvas,ctx,`${players[i].bet}$`,[255,255,255],betPos,30,'JqkasWild-w1YD6');
            }

            for (const entry of disconnectedPlayersChips) {
                drawText(canvas,ctx,`${entry.value}$`,[255,255,255],entry.pos,30,'JqkasWild-w1YD6');
            }

            const roundTextPos = [-0.8, 0.8];
            drawText(canvas,ctx,`Round ${currGameState.round}`,[255,255,255],roundTextPos,30,'JqkasWild-w1YD6');

            animationId = requestAnimationFrame(loop);
        }
        animationId = requestAnimationFrame(loop);
        return () => cancelAnimationFrame(animationId);
    },[assetsLoaded,ctxRef,currGameState,prevGameState])

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
