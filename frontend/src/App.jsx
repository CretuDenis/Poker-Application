import {BrowserRouter,Routes,Route,Navigate} from "react-router-dom"

import ProtectedRoute from "./components/ProtectedRoute";
import Form from "./components/Form.jsx"
import Home from "./pages/Home.jsx"
import Game from './pages/Game.jsx'

function Logout() {
    localStorage.clear()
    return <Navigate to ="/login" />
}

function Login() {
    return (
        <Form method="login" />
    );
}

function Register() {
    return (
        <Form method="register" />
    );

}

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"
                    element={
                        <ProtectedRoute>
                            <Home />                
                        </ProtectedRoute>
                    } />
                <Route path="/game/:gameId" element={
                    <ProtectedRoute>
                        <Game />
                    </ProtectedRoute>
                    } />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/logout" element={<Logout />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App
