import { useNavigate } from "react-router-dom"
import { useState } from 'react'
import { useWebSockets } from '../hooks/useWebSockets.jsx'
import { usernameFromToken } from '../api'
import "./Home.css"

function Home() {
    const [searching, setSearching] = useState(false);
    const navigate = useNavigate();
    const { sendMessage, connected } = useWebSockets((message) => {
        if ("game" in message) navigate(`/game/${message.game}`)
        else console.log(message);
    });

    const handleSearchForGame = () => {
        setSearching(!searching);
        sendMessage("/app/queue", {
            type: "QueueMessage",
            content: { info: searching ? "leave" : "join" },
        });
    }

    const handleJoin = () => {

    }

    const handleCreatePrivate = () => {

    }

    return (
        <div className="home-page">
            <div className="home-card">
                <div className="home-suits">♠ ♥ ♣ ♦</div>
                <h2 className="home-title">Welcome, {usernameFromToken()}</h2>
                <div className="home-body">
                    <button className="home-btn" disabled={!connected} onClick={handleSearchForGame}>
                        {searching ? 'Cancel search...' : 'Search for game'}
                    </button>
                    <button className="home-btn" disabled={!connected} onClick={handleCreatePrivate}>
                        Create private game
                    </button>
                    <div className="home-divider" />
                    <div className="home-join-row">
                        <input className="home-input" type="text" placeholder="Enter room code" />
                        <button className="home-btn" disabled={!connected} onClick={handleJoin} >Join</button>
                    </div>
                    <div className="home-divider" />
                    <button className="home-btn logout" onClick={() => navigate("/logout")}>Logout</button>
                </div>
            </div>
        </div>
    );
}

export default Home;
