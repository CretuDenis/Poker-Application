import { useNavigate } from "react-router-dom"
import { useState } from 'react'
import { useWebSockets } from '../hooks/useWebSockets.jsx'
import { usernameFromToken } from '../api'

function Home() {
    const [searching,setSearching] = useState(false);    
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
            Hello {usernameFromToken()} 
            <button onClick = { () => navigate("/logout") }>Logout</button>
            <button disabled={!connected} onClick = {handleSearchForGame}>{ (searching) ? '...' : 'Search for game'}</button>
        </div>
    );
}

export default Home;
