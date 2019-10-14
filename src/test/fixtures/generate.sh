#!/usr/bin/env bash

set -eo pipefail

rm -f crypto-material/channel.tx
rm -f crypto-material/core.yaml
rm -f crypto-material/genesis.block
rm -f crypto-material/mychannel.block
rm -rf crypto-material/crypto-config  

cd docker-compose

docker-compose -f docker-compose-cli.yaml up -d
docker exec cli cryptogen generate --config=/etc/hyperledger/config/crypto-config.yaml --output /etc/hyperledger/config/crypto-config
docker exec cli configtxgen -profile TwoOrgsOrdererGenesis -outputBlock /etc/hyperledger/config/genesis.block -channelID testchainid
docker exec cli configtxgen -profile TwoOrgsChannel -outputCreateChannelTx /etc/hyperledger/config/channel.tx -channelID mychannel
docker exec cli configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate /etc/hyperledger/config/Org1MSPanchors.tx -channelID mychannel -asOrg Org1MSP
docker exec cli configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate /etc/hyperledger/config/Org2MSPanchors.tx -channelID mychannel -asOrg Org2MSP
docker exec cli cp /etc/hyperledger/fabric/core.yaml /etc/hyperledger/config
docker exec cli sh /etc/hyperledger/config/rename_sk.sh
docker-compose -f docker-compose-cli.yaml down --volumes
