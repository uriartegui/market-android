import {
  Controller,
  Post,
  Get,
  Param,
  Body,
  UseGuards,
} from '@nestjs/common';
import { PaymentsService } from './payments.service';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@Controller('payments')
export class PaymentsController {
  constructor(private paymentsService: PaymentsService) {}

  @Post('pix/:orderId')
  @UseGuards(JwtAuthGuard)
  createPix(
    @Param('orderId') orderId: string,
    @CurrentUser() user: { id: string },
  ) {
    return this.paymentsService.createPixPayment(orderId, user.id);
  }

  @Get('status/:orderId')
  @UseGuards(JwtAuthGuard)
  getStatus(@Param('orderId') orderId: string) {
    return this.paymentsService.getPaymentStatus(orderId);
  }

  @Post('webhook')
  webhook(@Body() data: any) {
    return this.paymentsService.handleWebhook(data);
  }
}
