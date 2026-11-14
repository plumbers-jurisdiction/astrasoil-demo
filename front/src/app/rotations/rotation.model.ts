export interface RotationRequest {
  latitude: number;
  longitude: number;
  history: { year: number; plants: string[] }[];
}
export interface CoverCrop {
  plant: string;
  reason: string;
}
export interface RotationYear {
  year: number;
  plant: string;
  coverCrop?: CoverCrop | null;
}
export interface RotationPlan {
  title: string;
  years: RotationYear[];
  justifications: string[];
  risks: string[];
  reviewPoints: string[];
}
