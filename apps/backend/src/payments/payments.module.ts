import { Module } from '@nestjs/common';
import { PaymentsService } from './payments.service';
import { PaymentsController } from './payments.controller';

import { PaymentExpiryScheduler } from './payment-expiry.scheduler';

@Module({
  providers: [PaymentsService, PaymentExpiryScheduler],
  controllers: [PaymentsController],
})

export class PaymentsModule {}
