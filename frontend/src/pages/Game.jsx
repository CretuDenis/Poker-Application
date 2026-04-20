import { useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect,useRef } from 'react';
import { useWebSockets } from "../hooks/useWebSockets"
import { usernameFromToken } from '../api'
import Canvas from "../components/Canvas.jsx"

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

class Hand {
    constructor(first = null,second = null) {
        this.first = first;
        this.second = second;
    }
}

const getClientBalance = (gameState) => {
    if (gameState === null) return null;
    const clientName = usernameFromToken();
    for(const player of gameState.players) {
        if(player.name === clientName) return player.balance;
    }
    return null;
}

function Game() {
    const assetMapRef = useRef(new Map());
    const assetsLoaded = useRef(false);

    const [ gameState, setGameState ] = useState(null);
    const [ prevGameState, setPrevGameState ] = useState(null);

    const { gameId } = useParams();
    const [ raiseAmount, setRaiseAmount] = useState(0);

    const navigate = useNavigate();

    useEffect(() => {
        const assetMap = assetMapRef.current;
        for(const audioName of audioNames) {
            const audio = new Audio(`/${audioDirectory}/${audioName}.ogg`);
            assetMap.set(audioName, audio); 
        }
        assetsLoaded.current = true;
    },[]);


    const {sendMessage, connected, subscribe } = useWebSockets((message) => {
        switch(message.type) {
            case "GameStateDTO": {
                setGameState(message.content.current);
                setPrevGameState(message.content.previous);
                break;
            }
            case "PokerDTO": {
                setGameState(message.content);
                break;
            }
        }
    });

    useEffect(() => {
        if (!connected) return;
        sendMessage(`/app/game/${gameId}`,{ type: "StateQuery", content: null });
    }, [connected]);

    const handleDisconnect = () => {
        sendMessage(`/app/game/${gameId}`, { type: 'DisconnectRequest', content: null });
        navigate("/");
    }

    const clientHand = () => {
        const clientName = usernameFromToken();
        if (gameState === null) {
            console.log("Cannot get user hand if gameState is null");
            return null;
        }
        for(const player of gameState.players) {
            if (player.name === clientName) return player.hand;
        }
        console.log("Client not found to retrieve the hand");
        return null;
    }

    const handleAction = (action) => {
        return () => {
            const clientName = usernameFromToken();
            const currentSpeaking = gameState.speaking;
            if (clientName !== currentSpeaking) return;

            const assetMap = assetMapRef.current;
            if (currentSpeaking === clientName && ("CALL" || action === "RAISE" || action === "ALLIN")) {
                const randomIndex = Math.floor(Math.random() * 2 + 1);
                const sound = assetMap.get(`chipsCollide${randomIndex}`);
                sound.currentTime = 0;
                sound.play();
            } else if (currentSpeaking === clientName && action === 'FOLD' && clientHand().first !== null) {
                const randomIndex = Math.floor(Math.random() * 2 + 1);
                const sound = assetMap.get(`cardSlide${randomIndex}`);
                sound.currentTime = 0;
                setHand(new Hand()); 
            }
            const message = {
                type : "MoveDTO",
                content : {
                    action : action,
                    amount : (action === "RAISE" ? raiseAmount : null), 
                },
            };
            sendMessage(`/app/game/${gameId}`,message);
        }
    }

    const shouldAskForReveal = () => {
        if (gameState === null) return false;
        const players = gameState.players;
        let numActive = 0;
        for(const player of players) {
            if (player.hand !== null) numActive++;
            if (numActive > 1) return false; 
        }
        const clientName = usernameFromToken();
        for(const player of players) {
            if (player.hand !== null && player.name !== clientName) return false;
        }
        return true;
    }

    return (
        <div>
            <Canvas currGameState={gameState} prevGameState={prevGameState} clientHand={clientHand()} />
                <div style={{ position: 'relative', zIndex: 1 }}> 
                    <h1>Game {gameId}</h1> 
                    <h1>Hello {usernameFromToken()}</h1> 
                    <button onClick = {handleAction("CHECK")}>Check</button>
                    <button onClick = {handleAction("CALL")}>Call</button>
                    <button onClick = {handleAction("RAISE")}>Raise</button>
                    <input 
                        type="range" 
                        min="1" 
                        max={getClientBalance(gameState)} 
                        value={raiseAmount}
                        onChange={(e) => setRaiseAmount(Number(e.target.value))} 
                    />
                    <span>{raiseAmount}</span>
                    <button onClick = {handleAction("ALLIN")}>All in</button>
                    <button onClick = {handleAction("FOLD")}>Fold</button>
                    <button onClick = {handleDisconnect}>Disconnect</button>
                    {
                        shouldAskForReveal() ? 
                            <div>
                                <h1>Reveal?</h1> 
                                <button onClick = {handleAction("REVEAL")}>Yes</button>
                                <button onClick = {handleAction("DONT_REVEAL")}>No</button>
                            </div>
                        : <></>
                    }
                    <pre>{JSON.stringify(gameState, null, 3)}</pre>
                    <pre>{JSON.stringify(prevGameState, null, 3)}</pre>
                </div>
        </div>
    );
}


export default Game; 
