import { useEffect, useRef, useState } from "react"
import { Client } from "@stomp/stompjs"
import { ACCESS_TOKEN } from "../constants"

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
                client.subscribe("/topic/game", (message) => {
                    onMessage(JSON.parse(message.body))
                })
                client.subscribe("/user/queue/private", (message) => {
                    onMessage(JSON.parse(message.body))
                })
                client.subscribe("/user/queue/reply", (message) => {
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

    const sendMessage = (destination: string, body: object) => {
        clientRef.current?.publish({
            destination,
            body: JSON.stringify(body)
        })
    }

    return { sendMessage, connected }
}

export default useWebSocket
