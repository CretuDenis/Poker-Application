import { useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import api from "../api"
import useWebSocket from "../hooks/useWebSocket"

function GameRoom() {
    const { gameId } = useParams();
    const [ gameState, setGameState ] = useState(null);
    const [ raiseAmount, setRaiseAmount] = useState(0);
    const navigate = useNavigate();

    const {sendMessage,connected, subscribe } = useWebSocket((message : any) => {
        console.log(message);
    });
 
    useEffect(() => {
        if(!connected) return;

        const unsub = subscribe(`/topic/game/${gameId}`,(message) => {
            setGameState(message);
        })

        const fetchState = async () => {
            try {
                const res = await api.get(`/game/${gameId}`);
                setGameState(res.data);
            }catch(e) {
                alert(e);
            }

        }
        fetchState();

    },[gameId,connected]);

    const handleDisconnect = () => {
        sendMessage(`/app/game/${gameId}/disconnect`,{"":""});    
        navigate("/");
    }

    const handleAction = (action : string) => {
        return () => {
            let message = { "action" : action, "amount" : (action === "RAISE" ? raiseAmount : null)};
            sendMessage(`/app/game/${gameId}/move`,message);
        }
    }

    return (
        <div>
            <h1>Game {gameId}</h1> 
            <button onClick = {handleAction("CHECK")}>Check</button>
            <button onClick = {handleAction("CALL")}>Call</button>
            <button onClick = {handleAction("RAISE")}>Raise</button>
            <input type="number" value={raiseAmount} onChange={(e) => setRaiseAmount(Number(e.target.value))} />
            <button onClick = {handleAction("ALLIN")}>All in</button>
            <button onClick = {handleAction("FOLD")}>Fold</button>
            <button onClick = {handleDisconnect}>Disconnect</button>
            <pre>{JSON.stringify(gameState, null, 3)}</pre>
        </div>
    );
}

export default GameRoom; 
