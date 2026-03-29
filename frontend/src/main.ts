import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'


document.querySelector<HTMLDivElement>('#app')!.innerHTML = `
    <div>Hello world </div>
`
const client = new Client({
    webSocketFactory: () => new SockJS('ws://localhost:8080/ws/websocket'),

    onConnect: () => {
        console.log('Connected!')
        client.subscribe('/topic/', (message) => {
            const body = JSON.parse(message.body)
            console.log('Received:', body)
        })

        client.publish({
            destination: '/app/send',
            body: JSON.stringify({ text: 'Hello Spring Boot!' })
        })
    },

    onDisconnect: () => console.log('Disconnected'),
    onStompError: (frame) => console.error('Error:', frame)
})

client.activate()

