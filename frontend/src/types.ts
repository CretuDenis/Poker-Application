

export class Message<T> {
    type : string;
    content : object;
    constructor(content : object, objType : new (...args: any[]) => T) {
        this.type = objType.name;
        this.content = content;
    }
}

export class StateQuery {}

export class DisconnectRequest {}

type MoveType = "RAISE" | "FOLD" | "CHECK" | "ALLIN" | "CALL";
type Amount = number | null;

export class MoveDTO {
    action : MoveType;
    amount : Amount;

    constructor(action : string, amount : Amount) {
        this.action = action as MoveType;
        this.amount = amount;
    }
}

export class QueueMessage {
    info : string;
    constructor(info : string) {
        this.info = info;
    }
}

export interface Card {
    symbol : string;
    suit : string;
}

export interface PokerPlayer {
    name : string;
    balance : number;
    bet : number;
    first? : Card;
    second? : Card;
}

export interface GameState {
    players : PokerPlayer[];
    communityCards : Card[];
    button : string;
    speaking : string;
}

export function isGameState(obj : unknown) : boolean {
    return (
        typeof obj === "object" &&
            obj !== null &&
            "players" in obj &&
            "communityCards" in obj &&
            "button" in obj &&
            "speaking" in obj
    );
}
