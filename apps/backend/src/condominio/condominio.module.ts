import { Module } from '@nestjs/common';
import { CondominioService } from './condominio.service';
import { CondominioController } from './condominio.controller';

@Module({
  providers: [CondominioService],
  controllers: [CondominioController],
  exports: [CondominioService],
})
export class CondominioModule {}
