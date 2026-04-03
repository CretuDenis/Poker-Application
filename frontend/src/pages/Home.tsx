import { useNavigate} from 'react-router-dom';
import { useState, useEffect } from 'react';
import { jwtDecode } from "jwt-decode"
import useWebSocket from "../hooks/useWebSocket"
import { ACCESS_TOKEN,REFRESH_TOKEN } from '../constants';

function Home() {
    const [searching,setSearching] = useState<boolean>(false);    
    const [username, setUsername] = useState<string>(() => {
        const access = localStorage.getItem(ACCESS_TOKEN);
        if (!access) return "Guest";
        return jwtDecode(access).sub ?? "Guest";
    });
    const navigate = useNavigate();


    const {sendMessage,connected} = useWebSocket((message : unknown) => {
        console.log(message);
        console.log("Message recieved");
    });
    
    const handleLogout = () => {
        localStorage.removeItem(ACCESS_TOKEN);
        localStorage.removeItem(REFRESH_TOKEN);
        navigate("/login");
    }


    const handleSearchForGame = () => { 
        setSearching(!searching);
        if(searching) {
            sendMessage("/app/queue/leave",{"":""});
        } else {
            sendMessage("/app/queue/join",{"":""});
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
