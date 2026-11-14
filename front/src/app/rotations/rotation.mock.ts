import { CROP_IMAGES } from './crop-images';
import { RotationResponse } from './rotation.model';

/** UI fixtures only; replace with the API response, not agronomic advice. */
export function demoRotations(startYear: number): RotationResponse {
  return {
    rotations: [
      {
        id: 'soil-health',
        title: 'Solo mais saudável',
        seasons: [
          { year: startYear, crop: 'Soja', image: CROP_IMAGES.soja, winterCrop: 'Ervilhaca' },
          { year: startYear + 1, crop: 'Milho', image: CROP_IMAGES.milho, winterCrop: 'Trigo' },
          { year: startYear + 2, crop: 'Soja', image: CROP_IMAGES.soja, winterCrop: 'Ervilhaca' },
        ],
        justifications: [
          'Exemplo de alternância entre culturas de famílias diferentes.',
          'A proposta inclui culturas de inverno nos intervalos entre as safras.',
        ],
        risks: ['Riscos ainda não avaliados para esta área.'],
        needsReview: [
          'Confirmar as janelas de semeadura e o manejo das coberturas.',
          'Verificar disponibilidade de sementes e histórico de pragas.',
          'Validar clima, análise de solo e adequação local com assistência técnica.',
        ],
      },
      {
        id: 'crop-diversity',
        title: 'Diversificação de culturas',
        seasons: [
          { year: startYear, crop: 'Milho', image: CROP_IMAGES.milho, winterCrop: 'Aveia' },
          { year: startYear + 1, crop: 'Feijão', image: CROP_IMAGES.feijao, winterCrop: 'Centeio' },
          { year: startYear + 2, crop: 'Sorgo', image: CROP_IMAGES.sorgo, winterCrop: null },
        ],
        justifications: [
          'Exemplo alternativo para comparar uma sequência com três culturas principais.',
        ],
        risks: ['Custos, mercado e demanda de água ainda não foram avaliados.'],
        needsReview: [
          'Confirmar viabilidade das culturas na região.',
          'Revisar manejo, equipamentos e calendário de cada safra.',
        ],
      },
    ],
  };
}
