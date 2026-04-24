import { useState } from "react"
import api from "../api"
import { useNavigate } from "react-router-dom"
import { ACCESS_TOKEN, REFRESH_TOKEN } from "../constants"
import "./Form.css"

function Form({ method }) {
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
            const res = await api.post(route, { username, password })
            if (method === "login") {
                localStorage.setItem(ACCESS_TOKEN, res.data.access)
                localStorage.setItem(REFRESH_TOKEN, res.data.refresh)
                navigate("/")
            } else {
                navigate("/login")
            }
        } catch (error) {
            alert(error)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="form-page">
            <div className="form-card">
                <div className="form-suits">♠ ♥ ♣ ♦</div>
                <h2 className="form-title">{name}</h2>
                <form onSubmit={handleSubmit} className="form-body">
                    <div className="form-group">
                        <label className="form-label">Username</label>
                        <input
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            type="text"
                            placeholder="Enter your username"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Password</label>
                        <input
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            type="password"
                            placeholder="Enter your password"
                            required
                        />
                    </div>
                    <button className="form-btn" type="submit">
                        {loading ? '...' : name}
                    </button>
                </form>
                {method === "login" && (
                    <p className="form-footer">
                        Don't have an account? <a className="form-link" href="/register">Register</a>
                    </p>
                )}
            </div>
        </div>
    )
}

export default Form
