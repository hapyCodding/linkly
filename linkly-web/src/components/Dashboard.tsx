import { useEffect, useRef, useState } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { getStats } from "../api";
import type { ClickEvent, StatsResponse } from "../types";
import { useClickStream } from "../useClickStream";

const DEVICE_COLORS: Record<string, string> = {
  desktop: "#6366f1",
  mobile: "#22d3ee",
  tablet: "#a78bfa",
  bot: "#f59e0b",
  unknown: "#64748b",
};

function hourLabel(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours()}시`;
}

function timeAgo(iso: string): string {
  const secs = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
  if (secs < 60) return `${Math.floor(secs)}초 전`;
  if (secs < 3600) return `${Math.floor(secs / 60)}분 전`;
  return `${Math.floor(secs / 3600)}시간 전`;
}

/** WS로 들어온 새 클릭을 통계 객체에 즉시 반영한다. */
function applyEvent(stats: StatsResponse, evt: ClickEvent): StatsResponse {
  const bump = (arr: { label: string; count: number }[], label: string) => {
    const idx = arr.findIndex((x) => x.label === label);
    if (idx >= 0) {
      const copy = [...arr];
      copy[idx] = { ...copy[idx], count: copy[idx].count + 1 };
      return copy.sort((a, b) => b.count - a.count);
    }
    return [...arr, { label, count: 1 }].sort((a, b) => b.count - a.count);
  };

  const timeline = [...stats.timeline];
  if (timeline.length > 0) {
    const last = timeline.length - 1;
    timeline[last] = { ...timeline[last], count: timeline[last].count + 1 };
  }

  return {
    ...stats,
    totalClicks: Math.max(stats.totalClicks + 1, evt.totalClicks),
    timeline,
    byCountry: bump(stats.byCountry, evt.country ?? "ZZ"),
    byDevice: bump(stats.byDevice, evt.device ?? "unknown"),
    recentClicks: [evt, ...stats.recentClicks].slice(0, 20),
  };
}

export function Dashboard({ code }: { code: string }) {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [flash, setFlash] = useState(false);
  const { status, events } = useClickStream(code);
  const lastTsRef = useRef<string>("");

  useEffect(() => {
    let alive = true;
    lastTsRef.current = "";
    setStats(null);
    getStats(code).then((s) => {
      if (!alive) return;
      lastTsRef.current = s.recentClicks[0]?.clickedAt ?? "";
      setStats(s);
    });
    return () => {
      alive = false;
    };
  }, [code]);

  useEffect(() => {
    if (events.length === 0) return;
    const fresh = events.filter((e) => e.clickedAt > lastTsRef.current);
    if (fresh.length === 0) return;
    lastTsRef.current = events[0].clickedAt;
    setStats((prev) => {
      if (!prev) return prev;
      // 오래된 것부터 반영해 순서 유지
      return [...fresh].reverse().reduce(applyEvent, prev);
    });
    setFlash(true);
    const t = setTimeout(() => setFlash(false), 500);
    return () => clearTimeout(t);
  }, [events]);

  if (!stats) return <div className="dash-empty">불러오는 중…</div>;

  return (
    <div className="dashboard">
      <div className="dash-header">
        <div>
          <div className="dash-code">/x/{code}</div>
          <div className={`total-clicks ${flash ? "flash" : ""}`}>
            {stats.totalClicks.toLocaleString()}
            <span className="total-label"> clicks</span>
          </div>
        </div>
        <span className={`ws-status ws-${status}`}>
          <span className="dot" />
          {status === "live" ? "LIVE" : status === "connecting" ? "연결 중" : "끊김"}
        </span>
      </div>

      <div className="chart-grid">
        <div className="chart-card wide">
          <h4>최근 24시간 클릭 추이</h4>
          <ResponsiveContainer width="100%" height={160}>
            <AreaChart data={stats.timeline} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="clickFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#6366f1" stopOpacity={0.7} />
                  <stop offset="100%" stopColor="#6366f1" stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <XAxis
                dataKey="time"
                tickFormatter={hourLabel}
                tick={{ fill: "#94a3b8", fontSize: 11 }}
                interval={3}
              />
              <YAxis allowDecimals={false} tick={{ fill: "#94a3b8", fontSize: 11 }} width={30} />
              <Tooltip
                labelFormatter={(v) => hourLabel(v as string)}
                contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8 }}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#818cf8"
                strokeWidth={2}
                fill="url(#clickFill)"
                isAnimationActive={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-card">
          <h4>국가별</h4>
          <ResponsiveContainer width="100%" height={160}>
            <BarChart data={stats.byCountry.slice(0, 6)} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
              <XAxis dataKey="label" tick={{ fill: "#94a3b8", fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fill: "#94a3b8", fontSize: 11 }} width={30} />
              <Tooltip
                cursor={{ fill: "rgba(148,163,184,0.08)" }}
                contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8 }}
              />
              <Bar dataKey="count" fill="#22d3ee" radius={[4, 4, 0, 0]} isAnimationActive={false} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-card">
          <h4>디바이스</h4>
          <ResponsiveContainer width="100%" height={160}>
            <PieChart>
              <Pie
                data={stats.byDevice}
                dataKey="count"
                nameKey="label"
                innerRadius={40}
                outerRadius={64}
                paddingAngle={2}
                isAnimationActive={false}
              >
                {stats.byDevice.map((d) => (
                  <Cell key={d.label} fill={DEVICE_COLORS[d.label] ?? "#64748b"} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8 }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="legend">
            {stats.byDevice.map((d) => (
              <span key={d.label} className="legend-item">
                <span className="legend-dot" style={{ background: DEVICE_COLORS[d.label] ?? "#64748b" }} />
                {d.label} {d.count}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="live-feed">
        <h4>실시간 클릭 피드</h4>
        {stats.recentClicks.length === 0 ? (
          <p className="empty small">
            아직 클릭이 없습니다. <code>{`/x/${code}`}</code> 를 새 탭에서 열어보세요.
          </p>
        ) : (
          <ul>
            {stats.recentClicks.map((c, i) => (
              <li key={`${c.clickedAt}-${i}`} className={i === 0 && flash ? "row-flash" : ""}>
                <span className="feed-device" style={{ color: DEVICE_COLORS[c.device ?? "unknown"] }}>
                  ● {c.device ?? "unknown"}
                </span>
                <span className="feed-country">{c.country ?? "??"}</span>
                <span className="feed-time">{timeAgo(c.clickedAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
