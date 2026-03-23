import { IsString, MinLength } from 'class-validator';

export class CreateCondominioDto {
  @IsString()
  @MinLength(3)
  name: string;

  @IsString()
  address: string;

  @IsString()
  @MinLength(4)
  code: string;
}
