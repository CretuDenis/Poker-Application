import { useEffect, useRef, useState } from "react"
import { Client, type StompSubscription } from "@stomp/stompjs"
import { ACCESS_TOKEN } from "../constants"
import type { Message } from "../types"

type SubscriptionHandle = {
    unsubscribe: () => void;
    onReceiptReceived: (callback: () => void) => void;
}

const useWebSocket = (onMessage: (message: unknown) => void) => {
    const clientRef = useRef<Client | null>(null)
    const onMessageRef = useRef(onMessage)
    const receiptCallbacksRef = useRef<Map<string, () => void>>(new Map())
    const [connected, setConnected] = useState(false)

    useEffect(() => {
        onMessageRef.current = onMessage
    }, [onMessage])

    useEffect(() => {
        const client = new Client({
            brokerURL: `${import.meta.env.VITE_WS_API_URL}/ws`,
            connectHeaders: {
                Authorization: `Bearer ${localStorage.getItem(ACCESS_TOKEN)}`
            },
            onConnect: () => {
                setConnected(true)
                client.subscribe("/user/queue/private", (message) => {
                    onMessageRef.current(JSON.parse(message.body))
                })
            },
            onDisconnect: () => setConnected(false),
            onStompError: (frame) => console.error("STOMP error", frame),
            onUnhandledReceipt: (frame) => {
                const receiptId = frame.headers["receipt-id"]
                const cb = receiptCallbacksRef.current.get(receiptId)
                if (cb) {
                    cb()
                    receiptCallbacksRef.current.delete(receiptId)
                }
            }
        })
        client.activate()
        clientRef.current = client
        return () => { client.deactivate() }
    }, [])

    const subscribe = (destination: string, callback: (message: any) => void): SubscriptionHandle => {
        const receiptId = `receipt-${destination}-${Date.now()}`

        const stompSub: StompSubscription | undefined = clientRef.current?.subscribe(
            destination,
            (message) => callback(JSON.parse(message.body)),
            { receipt: receiptId }
        )

        return {
            unsubscribe: () => stompSub?.unsubscribe(),
            onReceiptReceived: (cb: () => void) => {
                receiptCallbacksRef.current.set(receiptId, cb)
            }
        }
    }

    const sendMessage = <T>(destination: string, message: Message<T>) => {
        clientRef.current?.publish({
            destination,
            body: JSON.stringify(message)
        })
        console.log(message);
    }

    return { sendMessage, connected, subscribe }
}

export default useWebSocket
