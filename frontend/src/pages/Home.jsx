import { useNavigate } from "react-router-dom"
import { useState, useRef, useEffect } from 'react'
import { useWebSockets } from '../hooks/useWebSockets.jsx'
import { usernameFromToken } from '../api'
import "./Home.css"

function Home() {
    const navigate = useNavigate();
    const [searching, setSearching] = useState(false);
    const [createdRoomCode, setCreatedRoomCode] = useState(null);
    const [roomCode, setRoomCode] = useState(""); 
    const [joinedRoom, setJoinedRoom] = useState(null); 

    const roomCodeRef = useRef("");
    
    useEffect(() => {
        roomCodeRef.current = roomCode;
    }, [roomCode]);

    const { sendMessage, connected } = useWebSockets((message) => {
        if ("game" in message) {
            navigate(`/game/${message.game}`);
        } else if ("lobby" in message) {
            setCreatedRoomCode(message.lobby);
        } else if ("joined" in message && message.joined === "ok") {
            setJoinedRoom(roomCodeRef.current);
        } else {
            console.log(message);
        }
    });

    const handleSearchForGame = () => {
        setSearching(prev => !prev);
        sendMessage("/app/queue", {
            type: "QueueMessage",
            content: { info: searching ? "leave" : "join" },
        });
    }

    const handleJoin = () => {
        sendMessage("/app/queue", {
            type: "QueueMessage",
            content: { info: `join ${roomCode}` },
        });
    }

    const handleCreatePrivate = () => {
        if (createdRoomCode === null) {
            sendMessage("/app/queue", {
                type: "QueueMessage",
                content: { info: "create_private" },
            });
        } else {
            sendMessage("/app/queue", {
                type: "QueueMessage",
                content: { info: `leave ${createdRoomCode}` },
            });
            setCreatedRoomCode(null);
        }
    }

    const handleLeaveRoom = () => {
        sendMessage("/app/queue", {
            type: "QueueMessage",
            content: { info: `leave ${joinedRoom}` },
        });
        setJoinedRoom(null);
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
                        { createdRoomCode === null ? 'Create private game' : 'Leave private room' }
                    </button>

                    { createdRoomCode !== null && (
                        <input className="home-input" type="text" value={createdRoomCode} readOnly />
                    )}

                    <div className="home-divider" />

                    <div className="home-join-row">
                        {joinedRoom === null ? (
                            <>
                                <input 
                                    className="home-input" 
                                    type="text" 
                                    placeholder="Enter room code" 
                                    value={roomCode} 
                                    onChange={(e) => setRoomCode(e.target.value)} 
                                />
                                <button className="home-btn" disabled={!connected || !roomCode} onClick={handleJoin}>Join</button>
                            </>
                        ) : (
                            <button className="home-btn" disabled={!connected} onClick={handleLeaveRoom}>
                                Leave room ({joinedRoom})
                            </button>
                        )}
                    </div>

                    <div className="home-divider" />
                    <button className="home-btn logout" onClick={() => navigate("/logout")}>Logout</button>
                </div>
            </div>
        </div>
    );
}

export default Home;
