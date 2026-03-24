import { Controller, Post, Body, HttpCode } from '@nestjs/common';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { NotificationService } from '../notifications/notification.service';
import { TokenExpiryScheduler } from '../notifications/token-expiry.scheduler';

@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private tokenExpiryScheduler: TokenExpiryScheduler,
  ) {}

  @Post('register')
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(200)
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('refresh')
  @HttpCode(200)
  refresh(@Body('refreshToken') token: string) {
    return this.authService.refresh(token);
  }

  @Post('logout')
  @HttpCode(200)
  logout(@Body('refreshToken') token: string) {
    return this.authService.logout(token);
  }

  @Post('session-expired')
  @HttpCode(200)
  async sessionExpired(@Body('condominioId') condominioId?: string) {
    await this.notificationService.notifySessionExpired(condominioId);
    return { ok: true };
  }

  // Endpoint de teste — dispara o check de expiração manualmente
  @Post('test-expiry-check')
  @HttpCode(200)
  async testExpiryCheck() {
    await this.tokenExpiryScheduler.checkExpiringTokens();
    return { ok: true };
  }
}
