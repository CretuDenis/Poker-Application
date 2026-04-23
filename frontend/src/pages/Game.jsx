import { useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect,useRef } from 'react';
import { useWebSockets } from "../hooks/useWebSockets"
import { usernameFromToken } from '../api'
import Canvas from "../components/Canvas.jsx"

const getClientBalance = (gameState) => {
    if (gameState === null) return null;
    const clientName = usernameFromToken();
    for(const player of gameState.players) {
        if(player.name === clientName) return player.balance;
    }
    return null;
}

function Game() {
    const [ gameState, setGameState ] = useState(null);
    const prevGameStateRef = useRef(null);

    const { gameId } = useParams();
    const [ raiseAmount, setRaiseAmount] = useState(0);

    const navigate = useNavigate();

    const {sendMessage, connected, subscribe } = useWebSockets((message) => {
        switch(message.type) {
            case "GameStateDTO": {
                setGameState(message.content.current);
                prevGameStateRef.current = message.content.previous;
                break;
            }
            default: {
                console.log(`Intercepted unknown message type: ${message.type}`);
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

    const handleAction = (action) => {
        return () => {
            const clientName = usernameFromToken();
            const currentSpeaking = gameState.speaking;
            if (clientName !== currentSpeaking) return;

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

    const debug = false;

    return (
        <div>
            <Canvas currGameState={gameState} prevGameState={prevGameStateRef.current} />
                <div style={{ position: 'relative', zIndex: 1 }}> 
                    {
                        debug ?
                            <div>
                                <h1>Game {gameId}</h1> 
                                <h1>Hello {usernameFromToken()}</h1> 
                            </div>
                            :
                            <></>

                    }
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
                        debug ?
                            <div>
                                <pre>{JSON.stringify(gameState, null, 3)}</pre>
                                <pre>{JSON.stringify(prevGameStateRef.current, null, 3)}</pre>
                            </div>
                            :
                            <></>
                    }
                </div>
        </div>
    );
}


export default Game; 
