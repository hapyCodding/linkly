import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { WS_URL } from "./api";
import type { ClickEvent } from "./types";

export type WsStatus = "connecting" | "live" | "closed";

/**
 * 특정 링크(code)의 클릭 이벤트를 실시간 구독한다.
 * code 가 null 이면 전역 피드(/topic/clicks)를 구독한다.
 */
export function useClickStream(code: string | null) {
  const [status, setStatus] = useState<WsStatus>("connecting");
  const [events, setEvents] = useState<ClickEvent[]>([]);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    setEvents([]);
    setStatus("connecting");

    const topic = code ? `/topic/clicks/${code}` : "/topic/clicks";
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3000,
      onConnect: () => {
        setStatus("live");
        client.subscribe(topic, (msg) => {
          const evt = JSON.parse(msg.body) as ClickEvent;
          setEvents((prev) => [evt, ...prev].slice(0, 50));
        });
      },
      onWebSocketClose: () => setStatus("closed"),
      onStompError: () => setStatus("closed"),
    });

    client.activate();
    clientRef.current = client;
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [code]);

  return { status, events };
}
