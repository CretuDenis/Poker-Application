import { useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect,useRef } from 'react';
import { useWebSockets } from "../hooks/useWebSockets"
import { ACCESS_TOKEN } from '../constants';
import { jwtDecode } from "jwt-decode"
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

class Player {
    constructor(name,balance,bet) {
        this.name = name;
        this.balance = balance;
        this.bet = bet;
    }
}

class Card {
    constructor(symbol,suti) {
        this.symbol = symbol;
        this.suit = suit;
    }
}

class GameState {
    constructor(players = [], communityCards = [null,null,null,null,null], button = "", speaking = "", roundCount = 0) {
        this.players = players;
        this.communityCards = communityCards;
        this.button = button;
        this.speaking = speaking;
        this.roundCount = roundCount;
    }
}

class Hand {
    constructor(first = null,second = null) {
        this.first = first;
        this.second = second;
    }
}

const getClientBalance = (gameState) => {
    const clientName = usernameFromToken();
    for(const player of gameState.players) {
        if(player.name === clientName) return player.balance;
    }
    return null;
}

function Game() {
    const assetMapRef = useRef(new Map());
    const assetsLoaded = useRef(false);

    const gameStateRef = useRef(new GameState());
    const prevGameStateRef = useRef(new GameState());

    const { gameId } = useParams();
    const [ clientHand, setHand] = useState(new Hand());
    const [ raiseAmount, setRaiseAmount] = useState(0);
    const [username, setUsername] = useState(() => {
        const access = localStorage.getItem(ACCESS_TOKEN);
        if (!access) return "Guest";
        return jwtDecode(access).sub ?? "Guest";
    });

    const navigate = useNavigate();

    useEffect(() => {
        const assetMap = assetMapRef.current;
        for(const audioName of audioNames) {
            const audio = new Audio(`/${audioDirectory}/${audioName}.ogg`);
            assetMap.set(audioName, audio); 
        }

        assetsLoaded.current = true;
    },[]);

    const {sendMessage,connected, subscribe } = useWebSockets((message) => {
        switch(message.type) {
            case "PokerDTO": {
                prevGameStateRef.current = gameStateRef.current;
                gameStateRef.current = message.content;
                break;
            }
            case "HandDTO": {
                setHand(message.content);
                console.log(message.content);
                break;
            } 
        }
    });

    useEffect(() => {
        if (!connected) return;

        const sub = subscribe(`/topic/game/${gameId}`, (message) => {
            switch (message.type) {
                case "PokerDTO": {
                    prevGameStateRef.current = gameStateRef.current;
                    gameStateRef.current = message.content;
                    break;
                }
            }
        });

        sendMessage(`/app/game/${gameId}`,{ type: "StateQuery", content: null });
        sendMessage(`/app/game/${gameId}`,{ type: "HandQuery", content: null });

        return () => sub.unsubscribe();
    }, [connected]);

    const handleDisconnect = () => {
        sendMessage(`/app/game/${gameId}`,{});
        navigate("/");
    }

    const handleAction = (action) => {
        return () => {
            const clientName = usernameFromToken();
            const currentSpeaking = gameStateRef.current.speaking;
            const assetMap = assetMapRef.current;
            if (currentSpeaking === clientName && ("CALL" || action === "RAISE" || action === "ALLIN")) {
                const soundIndex = Math.floor(Math.random() * 2 + 1);
                const sound1 = assetMap.get(`chipsCollide1`);
                const sound2 = assetMap.get(`chipsCollide2`);
                const sound3 = assetMap.get(`chipsCollide3`);

                sound1.currentTime = 0;
                sound2.currentTime = 0;
                sound3.currentTime = 0;

                sound1.play();
                sound2.play();
                sound3.play();
            } else if (currentSpeaking === clientName && action === 'FOLD') {

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

    return (
        <div>
            <Canvas currGameState={gameStateRef.current} prevGameState={prevGameStateRef.current} clientHand={clientHand} />
                <div style={{ position: 'relative', zIndex: 1 }}> 
                    <h1>Game {gameId}</h1> 
                    <h1>Hello {username}</h1> 
                    <button onClick = {handleAction("CHECK")}>Check</button>
                    <button onClick = {handleAction("CALL")}>Call</button>
                    <button onClick = {handleAction("RAISE")}>Raise</button>
                    <input 
                        type="range" 
                        min="1" 
                        max={getClientBalance(gameStateRef.current)} 
                        value={raiseAmount}
                        onChange={(e) => setRaiseAmount(Number(e.target.value))} 
                    />
                    <span>{raiseAmount}</span>
                    <button onClick = {handleAction("ALLIN")}>All in</button>
                    <button onClick = {handleAction("FOLD")}>Fold</button>
                    <button onClick = {handleDisconnect}>Disconnect</button>
                    <pre>{JSON.stringify(gameStateRef.current, null, 3)}</pre>
                    <pre>{JSON.stringify(clientHand, null, 3)}</pre>
                </div>
        </div>
    );
}

export default Game; 
