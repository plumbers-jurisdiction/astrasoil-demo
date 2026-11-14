import { CROP_IMAGES } from './crop-images';
import { RotationResponse } from './rotation.model';

/** UI fixtures only; replace with the API response, not agronomic advice. */
export function demoRotations(startYear: number): RotationResponse {
  return {
    rotations: [
      {
        id: 'soil-health',
        title: 'Healthier soil',
        seasons: [
          { year: startYear, crop: 'Soybean', image: CROP_IMAGES.soja, winterCrop: 'Hairy vetch' },
          { year: startYear + 1, crop: 'Corn', image: CROP_IMAGES.milho, winterCrop: 'Wheat' },
          {
            year: startYear + 2,
            crop: 'Soybean',
            image: CROP_IMAGES.soja,
            winterCrop: 'Hairy vetch',
          },
        ],
        justifications: [
          'Example of alternating crops from different plant families.',
          'The plan includes winter crops between the main growing seasons.',
        ],
        risks: ['Risks have not yet been assessed for this area.'],
        needsReview: [
          'Confirm sowing windows and cover crop management.',
          'Check seed availability and pest history.',
          'Validate climate, soil tests, and local suitability with an agricultural adviser.',
        ],
      },
      {
        id: 'crop-diversity',
        title: 'Crop diversification',
        seasons: [
          { year: startYear, crop: 'Corn', image: CROP_IMAGES.milho, winterCrop: 'Oats' },
          {
            year: startYear + 1,
            crop: 'Dry bean',
            image: CROP_IMAGES.feijao,
            winterCrop: 'Cereal rye',
          },
          { year: startYear + 2, crop: 'Sorghum', image: CROP_IMAGES.sorgo, winterCrop: null },
        ],
        justifications: ['Alternative example for comparing a sequence of three main crops.'],
        risks: ['Costs, market conditions, and water demand have not yet been assessed.'],
        needsReview: [
          'Confirm crop suitability for the region.',
          'Review management, equipment, and the schedule for each season.',
        ],
      },
    ],
  };
}
