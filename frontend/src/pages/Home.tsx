import { useNavigate} from 'react-router-dom';
import { useState, useEffect } from 'react';
import useWebSocket from "../hooks/useWebSocket"
import { ACCESS_TOKEN,REFRESH_TOKEN } from '../constants';
import { jwtDecode } from "jwt-decode"
import { QueueMessage,Message } from "../types"

function Home() {
    const [searching,setSearching] = useState<boolean>(false);    
    const [username, setUsername] = useState<string>(() => {
        const access = localStorage.getItem(ACCESS_TOKEN);
        if (!access) return "Guest";
        return jwtDecode(access).sub ?? "Guest";
    });
    const navigate = useNavigate();

    const {sendMessage,connected, subscribe } = useWebSocket((message : any) => {
        if ("game" in message) {
            const gameId = message.game;
            navigate(`/game/${gameId}`)
        } else {
            console.log(message);
        }
    });
    
    const handleLogout = () => {
        localStorage.removeItem(ACCESS_TOKEN);
        localStorage.removeItem(REFRESH_TOKEN);
        navigate("/login");
    }

    const handleSearchForGame = () => { 
        setSearching(!searching);
        if(searching) {
            sendMessage<QueueMessage>("/app/queue",new Message<QueueMessage>(new QueueMessage("leave"),QueueMessage));
        } else {
            sendMessage<QueueMessage>("/app/queue",new Message<QueueMessage>(new QueueMessage("join"),QueueMessage));
        }
    }

    return (
        <div>
            Hello {username} 
            <button onClick = {handleLogout}>Logout</button>
            <button disabled={!connected} onClick = {handleSearchForGame}>{ (searching) ? '...' : 'Search for game'}</button>
        </div>
    );
}

export default Home; 
