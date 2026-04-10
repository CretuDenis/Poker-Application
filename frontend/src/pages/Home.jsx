import { jwtDecode } from "jwt-decode"
import { ACCESS_TOKEN } from "../constants"
import { useNavigate } from "react-router-dom"
import { useState } from 'react'
import { useWebSockets } from '../hooks/useWebSockets.jsx'

function Home() {
    const [searching,setSearching] = useState(false);    
    const [username, setUsername] = useState(() => {
        const access = localStorage.getItem(ACCESS_TOKEN);
        if (!access) return "Guest";
        return jwtDecode(access).sub ?? "Guest";
    });
    const navigate = useNavigate();

    const {sendMessage,connected, subscribe } = useWebSockets((message) => {
        if ("game" in message) {
            const gameId = message.game;
            navigate(`/game/${gameId}`)
        } else {
            console.log(message);
        }
    });

    const handleSearchForGame = () => { 
        setSearching(!searching);
        if(searching) {
            sendMessage("/app/queue",{
                type: "QueueMessage",
                content: {
                    info: "leave"
                },
            });
        } else {
            sendMessage("/app/queue",{
                type: "QueueMessage",
                content: {
                    info: "join"
                },
            });
        }
    }

    return (
        <div>
            Hello {username} 
            <button onClick = { () => navigate("/logout") }>Logout</button>
            <button disabled={!connected} onClick = {handleSearchForGame}>{ (searching) ? '...' : 'Search for game'}</button>
        </div>
    );
}

export default Home;
