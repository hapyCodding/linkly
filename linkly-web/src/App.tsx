import { useEffect, useState } from "react";
import "./App.css";
import { listLinks } from "./api";
import type { LinkResponse } from "./types";
import { CreateLinkForm } from "./components/CreateLinkForm";
import { LinkList } from "./components/LinkList";
import { Dashboard } from "./components/Dashboard";

export default function App() {
  const [links, setLinks] = useState<LinkResponse[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

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

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">🔗</span> Linkly
          <span className="tag">URL 단축 · 실시간 클릭 애널리틱스</span>
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
          <LinkList links={links} selectedCode={selected} onSelect={setSelected} />
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
