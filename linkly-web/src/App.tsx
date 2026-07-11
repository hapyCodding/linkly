import { useEffect, useState } from "react";
import "./App.css";
import { deleteLink, listLinks, updateTags } from "./api";
import type { LinkResponse } from "./types";
import { CreateLinkForm } from "./components/CreateLinkForm";
import { LinkList } from "./components/LinkList";
import { Dashboard } from "./components/Dashboard";

export default function App() {
  const [links, setLinks] = useState<LinkResponse[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [tagFilter, setTagFilter] = useState<string | null>(null);

  async function refresh() {
    try {
      const data = await listLinks();
      setLinks(data);
      setLoadError(null);
      if (!selected && data.length > 0) setSelected(data[0].code);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "목록 로드 실패");
    }
  }

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onCreated(link: LinkResponse) {
    setLinks((prev) => [link, ...prev]);
    setSelected(link.code);
  }

  async function onDelete(code: string) {
    try {
      await deleteLink(code);
      setLinks((prev) => {
        const next = prev.filter((l) => l.code !== code);
        // 삭제한 링크가 선택돼 있었으면 목록의 첫 항목으로 이동
        if (selected === code) setSelected(next.length > 0 ? next[0].code : null);
        return next;
      });
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "삭제 실패");
    }
  }

  async function onUpdateTags(code: string, tags: string[]) {
    try {
      const updated = await updateTags(code, tags);
      setLinks((prev) => prev.map((l) => (l.code === code ? updated : l)));
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "태그 수정 실패");
    }
  }

  const allTags = Array.from(new Set(links.flatMap((l) => l.tags))).sort();
  const activeFilter = tagFilter && allTags.includes(tagFilter) ? tagFilter : null;
  const visibleLinks =
    activeFilter === null ? links : links.filter((l) => l.tags.includes(activeFilter));

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">🔗</span> Linkly
          <span className="tag">URL 단축 · 실시간 클릭 애널리틱스</span>
          <span className="ver">v0.2 · auto-deploy</span>
        </div>
      </header>

      <main className="layout">
        <section className="left">
          <CreateLinkForm onCreated={onCreated} />
          {loadError && (
            <p className="form-error">
              ⚠ 백엔드에 연결할 수 없습니다 ({loadError}). API 서버(8081)가 떠 있는지 확인하세요.
            </p>
          )}
          {allTags.length > 0 && (
            <div className="tag-filter">
              <button
                className={`filter-pill ${activeFilter === null ? "active" : ""}`}
                onClick={() => setTagFilter(null)}
              >
                전체
              </button>
              {allTags.map((t) => (
                <button
                  key={t}
                  className={`filter-pill ${activeFilter === t ? "active" : ""}`}
                  onClick={() => setTagFilter(t)}
                >
                  #{t}
                </button>
              ))}
            </div>
          )}
          <LinkList
            links={visibleLinks}
            selectedCode={selected}
            onSelect={setSelected}
            onDelete={onDelete}
            onUpdateTags={onUpdateTags}
          />
        </section>

        <section className="right">
          {selected ? (
            <Dashboard code={selected} />
          ) : (
            <div className="dash-empty">
              왼쪽에서 링크를 선택하면 실시간 대시보드가 열립니다.
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
