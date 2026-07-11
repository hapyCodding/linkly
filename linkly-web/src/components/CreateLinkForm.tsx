import { useState, type FormEvent } from "react";
import { createLink } from "../api";
import type { LinkResponse } from "../types";

interface Props {
  onCreated: (link: LinkResponse) => void;
}

export function CreateLinkForm({ onCreated }: Props) {
  const [url, setUrl] = useState("");
  const [expiresInDays, setExpiresInDays] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const link = await createLink({
        url: url.trim(),
        expiresInDays: expiresInDays ? Number(expiresInDays) : null,
      });
      onCreated(link);
      setUrl("");
      setExpiresInDays("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "생성 실패");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="create-form" onSubmit={submit}>
      <div className="create-row">
        <input
          className="url-input"
          type="text"
          placeholder="긴 URL을 붙여넣으세요 (https://...)"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          required
        />
        <input
          className="expiry-input"
          type="number"
          min={1}
          placeholder="만료(일)"
          value={expiresInDays}
          onChange={(e) => setExpiresInDays(e.target.value)}
        />
        <button className="btn-primary" type="submit" disabled={loading}>
          {loading ? "생성 중…" : "단축하기"}
        </button>
      </div>
      {error && <p className="form-error">⚠ {error}</p>}
    </form>
  );
}
