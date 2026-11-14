/** JSON contract for a future rotation API response. */
export interface RotationSeason {
  year: number;
  crop: string;
  image: { src: string; author: string; source: string; license: string; licenseUrl: string };
  winterCrop: string | null;
}
export interface RotationPlan {
  id: string;
  title: string;
  seasons: RotationSeason[];
  justifications: string[];
  risks: string[];
  needsReview: string[];
}
export interface RotationResponse {
  rotations: RotationPlan[];
}
