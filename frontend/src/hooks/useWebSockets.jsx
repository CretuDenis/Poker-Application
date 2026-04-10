import { Client } from '@stomp/stompjs';
import { useRef, useState, useEffect } from 'react'
 
import { ACCESS_TOKEN } from '../constants'

export const useWebSockets = (onMessage) => {
    const clientRef = useRef(null);
    const onMessageRef = useRef(onMessage);
    const [connected, setConnected] = useState(false);
    const receiptCallbacksRef = useRef(new Map());

    useEffect(() => {
        onMessageRef.current = onMessage;
    },[onMessage]);

    useEffect(() => {
        const client = new Client({
            brokerURL: `${import.meta.env.VITE_WS_API_URL}/ws`,
            connectHeaders: {
                Authorization : `Bearer ${localStorage.getItem(ACCESS_TOKEN)}`
            },
            onConnect: () => {
                setConnected(true);
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
        
        });

        client.activate();
        clientRef.current = client;
        return () => {
            client.deactivate();
        }
    },[]);

    const sendMessage = (destination, message) => {
        clientRef.current?.publish({
            destination,
            body: JSON.stringify(message)
        })
        console.log(`LOG: Sent message: ${JSON.stringify(message)}`);
    }

    const subscribe = (destination,callback) => {
        const receiptId = `receipt-${destination}-${Date.now()}`

        const stompSub = clientRef.current?.subscribe(
            destination,
            (message) => callback(JSON.parse(message.body)),
            { receipt: receiptId }
        );

        return {
            unsubscribe: () => stompSub?.unsubscribe(),
            onReceiptReceived: (cb) => {
                receiptCallbacksRef.current.set(receiptId, cb);
            }
        }
    };

    return { connected, sendMessage, subscribe }
}
