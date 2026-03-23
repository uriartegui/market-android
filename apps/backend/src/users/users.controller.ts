import {
  Controller, Get, Put, Post,
  Body, UseGuards, Request,
} from '@nestjs/common';
import { UsersService } from './users.service';
import { UpdateUserDto } from './dto/update-user.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('me')
  getMe(@Request() req) {
    return this.usersService.findById(req.user.id);
  }

  @Put('me')
  update(@Request() req, @Body() dto: UpdateUserDto) {
    return this.usersService.update(req.user.id, dto);
  }

  @Post('kiosk')
  createKiosk(@Request() req, @Body('name') name: string) {
    return this.usersService.createKioskUser(req.user.condominioId, name);
  }

  @Get('kiosk')
  getKiosk(@Request() req) {
    return this.usersService.getKioskUser(req.user.condominioId);
  }
}
