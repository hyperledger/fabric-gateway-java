cd docker-compose
docker-compose -f docker-compose-cli.yaml up -d 
docker exec cli cryptogen generate --config=/etc/hyperledger/config/crypto-config.yaml --output /etc/hyperledger/config/crypto-config
docker exec cli configtxgen -profile TwoOrgsOrdererGenesis -outputBlock /etc/hyperledger/config/genesis.block
docker exec cli configtxgen -profile TwoOrgsChannel -outputCreateChannelTx /etc/hyperledger/config/channel.tx -channelID mychannel
docker exec cli cp /etc/hyperledger/fabric/core.yaml /etc/hyperledger/config
docker-compose -f docker-compose-cli.yaml down --volumes
cd ../crypto-material
sh rename_sk.sh
