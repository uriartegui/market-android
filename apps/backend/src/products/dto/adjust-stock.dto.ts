import { IsEnum, IsNumber, IsOptional, IsString, Min } from 'class-validator';

export enum StockAdjustType {
  ADD = 'ADD',
  REMOVE = 'REMOVE',
  SET = 'SET',
}

export class AdjustStockDto {
  @IsNumber()
  @Min(0)
  quantity: number;

  @IsEnum(StockAdjustType)
  type: StockAdjustType;

  @IsOptional()
  @IsString()
  reason?: string;
}
