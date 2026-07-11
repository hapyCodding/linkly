import { useEffect, useState } from "react";
import { qrUrl } from "../api";
import type { LinkResponse } from "../types";

interface Props {
  links: LinkResponse[];
  selectedCode: string | null;
  onSelect: (code: string) => void;
  onDelete: (code: string) => void;
  onUpdateTags: (code: string, tags: string[]) => void;
  onUpdateMemo: (code: string, memo: string) => void;
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

function MemoEditor({
  memo,
  onSave,
}: {
  memo: string | null;
  onSave: (memo: string) => void;
}) {
  const [value, setValue] = useState(memo ?? "");
  useEffect(() => setValue(memo ?? ""), [memo]);
  return (
    <input
      className="memo-input"
      placeholder="📝 메모 추가…"
      value={value}
      onClick={(e) => e.stopPropagation()}
      onChange={(e) => setValue(e.target.value)}
      onBlur={() => {
        if (value !== (memo ?? "")) onSave(value);
      }}
      onKeyDown={(e) => {
        if (e.key === "Enter") (e.target as HTMLInputElement).blur();
      }}
    />
  );
}

function TagInput({ onAdd }: { onAdd: (tag: string) => void }) {
  const [value, setValue] = useState("");
  return (
    <input
      className="tag-input"
      placeholder="+ 태그"
      value={value}
      onClick={(e) => e.stopPropagation()}
      onChange={(e) => setValue(e.target.value)}
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          const tag = value.trim();
          if (tag) onAdd(tag);
          setValue("");
        }
      }}
    />
  );
}

export function LinkList({
  links,
  selectedCode,
  onSelect,
  onDelete,
  onUpdateTags,
  onUpdateMemo,
}: Props) {
  if (links.length === 0) {
    return <p className="empty">조건에 맞는 링크가 없습니다.</p>;
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
            {link.title && (
              <div className="link-title" title={link.title}>
                {link.title}
              </div>
            )}
            <div className="long-url" title={link.longUrl}>
              {link.longUrl}
            </div>
            <MemoEditor
              memo={link.memo}
              onSave={(memo) => onUpdateMemo(link.code, memo)}
            />
            <div className="tags-row" onClick={(e) => e.stopPropagation()}>
              {link.tags.map((tag) => (
                <span key={tag} className="tag-chip">
                  {tag}
                  <button
                    className="tag-x"
                    title="태그 제거"
                    onClick={() =>
                      onUpdateTags(
                        link.code,
                        link.tags.filter((t) => t !== tag),
                      )
                    }
                  >
                    ×
                  </button>
                </span>
              ))}
              <TagInput
                onAdd={(tag) => {
                  if (!link.tags.includes(tag)) {
                    onUpdateTags(link.code, [...link.tags, tag]);
                  }
                }}
              />
            </div>
          </div>
          <div className="click-badge">
            <span className="click-num">{link.clickCount}</span>
            <span className="click-label">clicks</span>
          </div>
          <button
            className="btn-delete"
            title="링크 삭제"
            onClick={(e) => {
              e.stopPropagation();
              if (window.confirm(`/x/${link.code} 링크와 클릭 기록을 삭제할까요?`)) {
                onDelete(link.code);
              }
            }}
          >
            ×
          </button>
        </li>
      ))}
    </ul>
  );
}
