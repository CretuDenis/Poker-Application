import { useEffect, useRef, useState } from "react"
import { Client } from "@stomp/stompjs"
import { ACCESS_TOKEN } from "../constants"

export interface Message {
    type : string;
    payload : object;
}

const useWebSocket = (onMessage: (message: unknown) => void) => {
    const clientRef = useRef<Client | null>(null)
    const [connected, setConnected] = useState(false)

    useEffect(() => {
        const client = new Client({
            brokerURL: `${import.meta.env.VITE_WS_API_URL}/ws`,
            connectHeaders: {
                Authorization: `Bearer ${localStorage.getItem(ACCESS_TOKEN)}`
            },
            onConnect: () => {
                setConnected(true)
                client.subscribe("/user/queue/private", (message) => {
                    onMessage(JSON.parse(message.body))
                })
            },
            onDisconnect: () => setConnected(false),
            onStompError: (frame) => console.error("STOMP error", frame)
        })

        client.activate()
        clientRef.current = client

        return () => {
            client.deactivate()
        }
    }, [])

    const subscribe = (destination: string, callback: (message: any) => void) => {
        return clientRef.current?.subscribe(destination, (message) => {
            callback(JSON.parse(message.body))
        })
    }

    const sendMessage = (destination: string, body: object) => {
        clientRef.current?.publish({
            destination,
            body: JSON.stringify(body)
        })
    }

    return { sendMessage, connected, subscribe }
}

export default useWebSocket
