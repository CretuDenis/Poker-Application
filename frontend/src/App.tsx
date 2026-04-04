//import { useState } from 'react'
import {BrowserRouter,Routes,Route} from "react-router-dom" 
import './App.css'
import ProtectedRoute from './components/ProtectedRoute.tsx'
import Home from "./pages/Home"
import GameRoom from "./pages/GameRoom"
import Form from "./components/Form"

function NotFound() {
    return (
        <div>Page not found 404</div>
    );
}

function Login() {
    return (
        <div>
            <Form route="/api/auth/login" method="login" />
        </div>
    );
}

function Register() {
    return (
        <div>
            <Form route="/api/auth/register" method="register" />
        </div>
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
                        <GameRoom />
                    </ProtectedRoute>
                    } />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App
