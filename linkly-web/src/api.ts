import type {
  CreateLinkRequest,
  LinkResponse,
  StatsResponse,
} from "./types";

// dev 서버에서는 8081 백엔드로, 배포(단일 컨테이너)에서는 빈 값 → same-origin(상대경로).
export const API_BASE =
  import.meta.env.VITE_API_BASE ?? "http://localhost:8081";

// 네이티브 WebSocket(STOMP) 엔드포인트. http->ws 변환.
// API_BASE가 비어 있으면(같은 오리진 배포) 현재 페이지 오리진에서 유도한다.
export const WS_URL =
  import.meta.env.VITE_WS_URL ??
  (API_BASE || window.location.origin).replace(/^http/, "ws") + "/ws";

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch {
      /* ignore */
    }
    throw new Error(msg);
  }
  return res.json() as Promise<T>;
}

export async function createLink(
  req: CreateLinkRequest,
): Promise<LinkResponse> {
  const res = await fetch(`${API_BASE}/api/links`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  return handle<LinkResponse>(res);
}

export async function listLinks(): Promise<LinkResponse[]> {
  return handle<LinkResponse[]>(await fetch(`${API_BASE}/api/links`));
}

export async function getStats(code: string): Promise<StatsResponse> {
  return handle<StatsResponse>(
    await fetch(`${API_BASE}/api/links/${code}/stats`),
  );
}

export async function deleteLink(code: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/links/${code}`, { method: "DELETE" });
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch {
      /* 204 No Content 등 본문 없음 */
    }
    throw new Error(msg);
  }
}

export async function updateTags(
  code: string,
  tags: string[],
): Promise<LinkResponse> {
  const res = await fetch(`${API_BASE}/api/links/${code}/tags`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ tags }),
  });
  return handle<LinkResponse>(res);
}

export async function updateMemo(
  code: string,
  memo: string,
): Promise<LinkResponse> {
  const res = await fetch(`${API_BASE}/api/links/${code}/memo`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ memo }),
  });
  return handle<LinkResponse>(res);
}

export function qrUrl(code: string): string {
  return `${API_BASE}/api/links/${code}/qr`;
}
