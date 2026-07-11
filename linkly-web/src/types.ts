export interface LinkResponse {
  code: string;
  longUrl: string;
  title: string | null;
  shortUrl: string;
  clickCount: number;
  createdAt: string;
  expiresAt: string | null;
  expired: boolean;
}

export interface LabelCount {
  label: string;
  count: number;
}

export interface TimeBucket {
  time: string;
  count: number;
}

export interface ClickEvent {
  code: string;
  clickedAt: string;
  country: string | null;
  device: string | null;
  referer: string | null;
  totalClicks: number;
}

export interface StatsResponse {
  code: string;
  totalClicks: number;
  byCountry: LabelCount[];
  byDevice: LabelCount[];
  timeline: TimeBucket[];
  recentClicks: ClickEvent[];
}

export interface CreateLinkRequest {
  url: string;
  expiresInDays?: number | null;
}
