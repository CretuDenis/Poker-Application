import { useNavigate, useParams} from 'react-router-dom';
import { useState, useEffect,useRef } from 'react';
import useWebSocket from "../hooks/useWebSocket"
import { ACCESS_TOKEN } from '../constants';
import { jwtDecode } from "jwt-decode"
import type { GameState } from "../types"
import { Message, StateQuery, DisconnectRequest, MoveDTO } from "../types"

async function initWebGPU(canvas: HTMLCanvasElement) {
    if (!navigator.gpu) throw new Error("WebGPU not supported");

    const adapter = await navigator.gpu.requestAdapter();
    if (!adapter) throw new Error("No GPU adapter found");

    const device = await adapter.requestDevice();
    const context = canvas.getContext("webgpu") as GPUCanvasContext;

    const format = navigator.gpu.getPreferredCanvasFormat();
    context.configure({
        device,
        format,
        alphaMode: "opaque"
    });

    return { device, context, format };
}

const vertices = new Float32Array([
  -0.8, -0.8,
   0.8, -0.8,
   0.8,  0.8,

  -0.8, -0.8,
   0.8,  0.8,
  -0.8,  0.8,
]);

async function render(device : GPUDevice, context : GPUCanvasContext,format : unknown) {
    const encoder = device.createCommandEncoder();

    const vertexBuffer = device.createBuffer({
      label: "Cell vertices",
      size: vertices.byteLength,
      usage: GPUBufferUsage.VERTEX | GPUBufferUsage.COPY_DST,
    });

    device.queue.writeBuffer(vertexBuffer,0, vertices);

    const vertexBufferLayout = {
      arrayStride: 8,
      attributes: [{
        format: "float32x2",
        offset: 0,
        shaderLocation: 0,
      }],
    };

    const cellShaderModule = device.createShaderModule({
      label: "Cell shader",
      code: `
        @vertex 
        fn vertexMain(@location(0) pos: vec2f) -> @builtin(position) vec4f {
            return vec4f(pos, 0, 1); // (X, Y, Z, W)
        }

        @fragment
        fn fragmentMain() -> @location(0) vec4f {
            return vec4f(1, 0, 0, 1); // (Red, Green, Blue, Alpha)
        }
      `
    });

    const cellPipeline = device.createRenderPipeline({
      label: "Cell pipeline",
      layout: "auto",
      vertex: {
        module: cellShaderModule,
        entryPoint: "vertexMain",
        buffers: [vertexBufferLayout]
      },
      fragment: {
        module: cellShaderModule,
        entryPoint: "fragmentMain",
        targets: [{
          format: format, 
        }]
      }
    });

    const pass = encoder.beginRenderPass({
        colorAttachments : [{
            view : context.getCurrentTexture().createView(),
            loadOp : "clear",
            clearValue: { r: 1, g: 1, b: 0, a: 1 },
            storeOp : "store"
        }]
    });

    pass.setPipeline(cellPipeline);
    pass.setVertexBuffer(0, vertexBuffer);
    pass.draw(vertices.length / 2); // 6 vertices

    pass.end();

    device.queue.submit([encoder.finish()])

}

function GameRoom() {
    const { gameId } = useParams();
    const [ gameState, setGameState ] = useState<GameState | null>(null);
    const [ raiseAmount, setRaiseAmount] = useState(0);
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [username, setUsername] = useState<string>(() => {
        const access = localStorage.getItem(ACCESS_TOKEN);
        if (!access) return "Guest";
        return jwtDecode(access).sub ?? "Guest";
    });

    const navigate = useNavigate();

    const {sendMessage,connected, subscribe } = useWebSocket((message : unknown) => {
        const msg = message as Message<unknown>;
        switch(msg.type) {
            case "PokerDTO": {
                const content = msg.content as GameState;
                setGameState(content);
            }
        }
        console.log(msg);
    });

    useEffect(() => {
        if (!connected) return;

        const sub = subscribe(`/topic/game/${gameId}`, (message: unknown) => {
            const msg = message as Message<unknown>;
            switch (msg.type) {
                case "PokerDTO": {
                    setGameState(msg.content as GameState);
                    break;
                }
            }
        });

        sendMessage<StateQuery>(`/app/game/${gameId}`,new Message<StateQuery>(new StateQuery(), StateQuery));

        return () => sub.unsubscribe();
    }, [gameId, connected]);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const init = async () => {
            const { device, context, format } = await initWebGPU(canvas);
            render(device, context,format);
        }

        init(); 
    }, []);

    const handleDisconnect = () => {
        sendMessage<DisconnectRequest>(`/app/game/${gameId}`,new Message<DisconnectRequest>(new DisconnectRequest(),DisconnectRequest));
        navigate("/");
    }

    const handleAction = (action : string) => {
        return () => {
            sendMessage<MoveDTO>(`/app/game/${gameId}`,new Message<MoveDTO>(new MoveDTO(action,action === "RAISE" ? raiseAmount : null),MoveDTO));
        }
    }

           // <h1>Game {gameId}</h1> 
           // <h1>Hello {username}</h1> 
           // <button onClick = {handleAction("CHECK")}>Check</button>
           // <button onClick = {handleAction("CALL")}>Call</button>
           // <button onClick = {handleAction("RAISE")}>Raise</button>
           // <input type="number" value={raiseAmount} onChange={(e) => setRaiseAmount(Number(e.target.value))} />
           // <button onClick = {handleAction("ALLIN")}>All in</button>
           // <button onClick = {handleAction("FOLD")}>Fold</button>
           // <button onClick = {handleDisconnect}>Disconnect</button>
           // <pre>{JSON.stringify(gameState, null, 3)}</pre>

    return (
        <div>
            <canvas ref={canvasRef} width={window.innerWidth} height={window.innerHeight} style={{ display: 'block' }}></canvas>
        </div>
    );
}

export default GameRoom; 
