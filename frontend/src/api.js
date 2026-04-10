import axios from "axios"
import { jwtDecode } from 'jwt-decode'
import { ACCESS_TOKEN } from "./constants"

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL
})

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem(ACCESS_TOKEN);
        if(token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

export const usernameFromToken = () => {
    const token = localStorage.getItem(ACCESS_TOKEN);
    
    if (!token) return "Guest";

    try {
        const decoded = jwtDecode(token);
        return decoded.sub;
    } catch (error) {
        console.error("Invalid token:", error);
        return "Guest";
    }
}

export default api
