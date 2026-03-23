import { Controller, Get, Post, Body, Param, UseGuards } from '@nestjs/common';
import { CondominioService } from './condominio.service';
import { CreateCondominioDto } from './dto/create-condominio.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@Controller('condominios')
export class CondominioController {
  constructor(private condominioService: CondominioService) {}

  @Post()
  @UseGuards(JwtAuthGuard)
  create(@Body() dto: CreateCondominioDto) {
    return this.condominioService.create(dto);
  }

  @Get()
  @UseGuards(JwtAuthGuard)
  findAll() {
    return this.condominioService.findAll();
  }

  @Get(':id')
  @UseGuards(JwtAuthGuard)
  findOne(@Param('id') id: string) {
    return this.condominioService.findOne(id);
  }

  @Post('join')
  @UseGuards(JwtAuthGuard)
  join(@CurrentUser() user: any, @Body('code') code: string) {
    return this.condominioService.join(user.id, code);
  }
}
