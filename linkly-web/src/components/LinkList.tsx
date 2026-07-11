import { useState } from "react";
import { qrUrl } from "../api";
import type { LinkResponse } from "../types";

interface Props {
  links: LinkResponse[];
  selectedCode: string | null;
  onSelect: (code: string) => void;
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      className="btn-ghost"
      onClick={(e) => {
        e.stopPropagation();
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 1200);
      }}
    >
      {copied ? "복사됨 ✓" : "복사"}
    </button>
  );
}

export function LinkList({ links, selectedCode, onSelect }: Props) {
  if (links.length === 0) {
    return <p className="empty">아직 링크가 없습니다. 위에서 하나 만들어 보세요 👆</p>;
  }
  return (
    <ul className="link-list">
      {links.map((link) => (
        <li
          key={link.code}
          className={`link-card ${selectedCode === link.code ? "selected" : ""}`}
          onClick={() => onSelect(link.code)}
        >
          <img className="qr-thumb" src={qrUrl(link.code)} alt="QR" width={64} height={64} />
          <div className="link-body">
            <div className="short-row">
              <a
                className="short-url"
                href={link.shortUrl}
                target="_blank"
                rel="noreferrer"
                onClick={(e) => e.stopPropagation()}
              >
                /x/{link.code}
              </a>
              <CopyButton text={link.shortUrl} />
              {link.expired && <span className="badge-expired">만료됨</span>}
            </div>
            <div className="long-url" title={link.longUrl}>
              {link.longUrl}
            </div>
          </div>
          <div className="click-badge">
            <span className="click-num">{link.clickCount}</span>
            <span className="click-label">clicks</span>
          </div>
        </li>
      ))}
    </ul>
  );
}
