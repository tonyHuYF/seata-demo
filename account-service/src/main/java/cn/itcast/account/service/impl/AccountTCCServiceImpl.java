package cn.itcast.account.service.impl;

import cn.itcast.account.entity.AccountFreeze;
import cn.itcast.account.mapper.AccountFreezeMapper;
import cn.itcast.account.mapper.AccountMapper;
import cn.itcast.account.service.AccountTCCService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service
public class AccountTCCServiceImpl implements AccountTCCService {

    @Resource
    private AccountMapper accountMapper;
    @Resource
    private AccountFreezeMapper accountFreezeMapper;


    @Override
    @Transactional
    public void deduct(String userId, int money) {
        String xid = RootContext.getXID();

        AccountFreeze accountFreeze = accountFreezeMapper.selectById(xid);

        if (accountFreeze != null) {
            return;
        }

        accountMapper.deduct(userId, money);
        AccountFreeze freeze = new AccountFreeze();
        freeze.setXid(xid);
        freeze.setUserId(userId);
        freeze.setFreezeMoney(money);
        freeze.setState(AccountFreeze.State.TRY);

        accountFreezeMapper.insert(freeze);

    }

    @Override
    public boolean confirm(BusinessActionContext ctx) {
        String xid = ctx.getXid();
        int count = accountFreezeMapper.deleteById(xid);
        return count == 1;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext ctx) {
        String xid = ctx.getXid();
        String userId = ctx.getActionContext("userId").toString();

        AccountFreeze accountFreeze = accountFreezeMapper.selectById(xid);

        if (accountFreeze == null) {
            AccountFreeze freeze = new AccountFreeze();
            freeze.setXid(xid);
            freeze.setUserId(userId);
            freeze.setFreezeMoney(0);
            freeze.setState(AccountFreeze.State.CANCEL);

            accountFreezeMapper.insert(freeze);
            return true;
        }

        if (accountFreeze.getState() == AccountFreeze.State.CANCEL) {
            //幂等处理
            return true;
        }

        accountMapper.refund(accountFreeze.getUserId(), accountFreeze.getFreezeMoney());

        accountFreeze.setState(AccountFreeze.State.CANCEL);
        accountFreeze.setFreezeMoney(0);
        int count = accountFreezeMapper.updateById(accountFreeze);

        return count == 1;
    }
}
