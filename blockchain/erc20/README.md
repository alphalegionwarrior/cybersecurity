# Tokendesign - Main token design mistakes
Main issues related to ERC20 standard tokens. ERC20 tokens have two critical issues that are already causing money losses. They are: 

Lack of transaction handling. 

There are two ways of performing a transaction in ERC20 tokens: transfer function.
approve + transferFrom mechanism.

Event handling is a standard practice in programming (Ethereum transaction should be considered an event): https://en.wikipedia.org/wiki/Event_(computing)

Unfortunately, ERC20 token standard lacks event (transaction) handling mechanism at all.

The transfer function will not notify the recipient that transaction happened. The recipient will not be able to recognize the incoming transaction! I wrote this illustration of the process that is leading to unhandled transactions and, as a result, stuck tokens and lost money.

Token balance is just a variable inside token contract.

Transaction of a token is a change in the internal variables of the contract (the balance of the sender will be decreased and the balance of the recipient will be increased).

As a result, if the recipient is a contract, users must transfer their tokens using the approve +transferFrom mechanism, and if the recipient is an externally owned account address, users must transfer their tokens via the transfer function. If a user will make a mistake and choose a wrong function then the token will get stuck inside contract (contract will not recognize a transaction). There will be no way to extract stuck tokens.

Does anyone of ERC20 token developers believe that users will never make such mistakes when developing their tokens? Loss of tokens happens now and then.

approve + transferFrom mechanism is potentially insecure. Re-approval attack. approve + transferFrom mechanism allows double spents.
This is not relevant unless there will be a possibility to create custom token-exchanges like Bancor token changer.

A possible attack is described here https://drive.google.com/file/d/0ByMtMw2hul0EN3NCaVFHSFdxRzA/view   


The Most Typical Mistakes In Smart Contracts - Based On L4A Solidity Audits
https://www.l4a-soft.com/blog/post/most-typical-mistakes-smart-contracts-based-l4a-solidity-audits


