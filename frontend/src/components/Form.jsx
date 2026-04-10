import {useState} from "react"
import api from "../api"
import {useNavigate} from "react-router-dom"
import {ACCESS_TOKEN, REFRESH_TOKEN} from "../constants"

function Form({method}) {
    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")
    const [loading, setLoading] = useState(false)
    const name = method === "login" ? "Login" : "Register"
    const navigate = useNavigate()
    
    const handleSubmit = async (e) => {
        setLoading(true)
        e.preventDefault() 
        try {
            const route = method === "login" ? "/api/auth/login" : "/api/auth/register"
            const res = await api.post(route, {username, password}) 
            if(method === "login") {
                localStorage.setItem(ACCESS_TOKEN, res.data.access);
                localStorage.setItem(REFRESH_TOKEN, res.data.refresh);
                navigate("/")
            } else {
                navigate("/login")
            }
        } catch(error) {
            alert(error);
        } finally {
            setLoading(false);
        }
    }

      return (
          <div>
            <h2>{name}</h2>
              <form onSubmit={handleSubmit}>
                <label>Username</label>
                <input value={username} onChange = {(e) => setUsername(e.target.value)} type="text" id="username" placeholder="Enter your username" required />

                <label>Password</label>
                <input value={password} onChange = {(e) => setPassword(e.target.value)} type="password" id="password" placeholder="Enter your password" required />

                <button type="submit">{loading ? '...' : name}</button>
              </form>
              { method === "login" ? <p>Dont have an account? <a href="/register">Register</a></p> : <></> }
              
          </div>
      );
    
}

export default Form
