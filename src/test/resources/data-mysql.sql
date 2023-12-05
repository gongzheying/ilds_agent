delete
from tbl_ilds_transfer_site;

delete
from tbl_ilds_transfer_credentials;

insert into tbl_ilds_transfer_credentials (id, created_at, last_modified_at, version, private_key_name,
                                           private_key_passphrase, private_key_content,
                                           private_key_content_type, password, title, `type`)
values (1, null, null, 0, null, null, null, null, 'password', 'password-only', 2),
       (2, null, null, 0, 'ssh_host_rsa_key', 'password',
        0x2D2D2D2D2D424547494E204F50454E5353482050524956415445204B45592D2D2D2D2D0A6233426C626E4E7A614331725A586B74646A454141414141436D466C637A49314E69316A6448494141414147596D4E79655842304141414147414141414244417031524267570A64474C7261335473614F76613568414141414541414141414541414149584141414142334E7A6143317963324541414141444151414241414143415143366D5441306F6E56450A6847535045734877755461356F4B345147564B687A6B716B2F4D6173575A396774496F4C7271684C6E777449466D46687A77566F7675735A374655764138412F6837713655610A676D587968704E584B386E4F4164313571515A744679734658643830735079524A724F63394B7979586B34705A6A2F524E675A344F7951426E395262375571753064313445330A693270647241452F6D3366726D3847676A794B74756264496A54686F523059695777783131544B4C4A4C493944756865445658764D2B5054397439314C7066663357314752340A4F2B43786A523577724B4E5A31773868566B313459447366594254786B7A50526D534F63335A5656526B6A37796244746A496E6536777673584B374471564A6C4968355977760A5033694A364B4A61746977653966753864374E596E585539654A662B59424935794D636B67526F7163524F646C38676A4548552B424A564D734D766B7A433541754D4F5749700A414F6D2F6E5363566F464A7A4F506C524B676A7834514C6A74687A4F346C6933697975727A712B684F6B6553464D446F55465643433470626963665A336E576B6E5A62416A680A4B4E6477664E647933456E555339736155354A426F38785854784C6C304E4936386B6C4D4751446565596132496D3077484C30506F2B4A674877736B3552483963573945635A0A39645170727A527046476A6D3877474244654E6B67692B654A426B5146734E456735397673793763494D6878394179666446632F704972585463793254334B76426633466A4E0A625547524D4D6A75433746613977337047457A377737715956566931783946343477336C39744C4A664F66726735616F424F31697775536B4332676E474C67664B4E6C3763560A497349315A424E6A6B3748674B714C31634D522F636244344B37476231324178705658585A6B78456B3949774141423044627A4B334F307572317275643255314D70783976760A74456542587A796F353851543964304B612B66666C3130387761736D4B78646B32736E51584B744F6B4638422F35726A7165386C5437367A574773356A472B39744A366D74530A55565443583845654E794D4265345A35375A41564969487657582F6252614C4B646C777A7243345A644B38794B7253333069456432696F3046454131514B614B6E426775462F0A4D6654697565584737492F2B486668747533445A4B774F434E374874564737616B363679653174554D62435A746754454B75623230784741484B357342555065654F304D44350A2B774D6B6C4D3571544A2F6A546E30446D31774F656131534B6365566950507258726E4B4361447A4572546476694959363143694F77526C557141382B7874622B482F666B340A4953624D6B50324B4864586A67684461354E356A72664F4574317242343979774A4C614732724B63476939517045414669785578437839683065645A77396C344D30596C4E570A6657424966694853434E6C4636302B6A413570494A437637474262567374334F684E6C55316B516D436C3441323961446669687148356D524A69393268546D4E6F5559412B450A6F59577978723950434E423636664A4D77385644526A71304C474536366A6C674D357634674D4657542F784C2F4878622B4B6F597A6F5532397867593443453755664454614B0A6242332B5277505642794B69524662704B77683053784155723754726A2F494C6352784274564B73766E396D42422B4767306D3769674673534E62692F54584E4543533264760A6552366762626B354F745034574834556A6F61687151654D70624D696B4A30786D6B7163584547585256787A496A6238584B396C394A3168574557655651754F63495969374D0A384A3866586B305A626E4A796F7455314D4664757A49636944727A75714431666865617442362F4B665057616951554A6F316870524747416762332F4371735779302F4B35750A47585836793134494639487038304E5044586D69714B5A78613947304933613544616134786766796656766C5564586873655868614F4F464135366E6E6A586C327465334E6E0A655866554A584B37434E2F75786B3843344456486E4A65543230336C317A6275424342383358526F2B576F71747575713866357A2B426F5772754E32715448584735535977760A6276544B366751416F4F355150422B777A787672434337756F446B6642766B684D4E5055756261714976394970625643305330424C2F4B546A6F47395637447A436B674436610A4E6A476666577565356570774B54796D78724651374A336668416F4E483732514F4136492F396C4F6C5651496C7A73755A412B44397A476A413446354F784852394B432B51520A3459314F56554A59654C672B396E6E587147416F766445536F4F705775384B6D354859316752315247766D635039764B3644726F4C42354E3761784C6A5657415157733574530A3946556C51786C696B63676778496365352F4F67563142577A594F4D4957666C5549744C364F534E48473838736D4335316B7434656266667A434C446F69536F6D77637841380A7961535A376345352F4851657768716B647176613547736556352F43446D482B77384F4175323532496B716279736C73617A6946486F4A715A5755636F796D546D586F342B750A726F75436C575571442F53615449595473475A70655563486254494D2B694A79674367444D2B4766724E534A6556722F6143384156707761676D69466A4A7735654E307664580A4C54326F6C4A4D59707336736D45326E376C7648625A742F69537A2B596637336B4C35706665736B356A346E32366279355044743553566C4F716949766E7A45424A786875650A6750546C3735504343316B695A544D796632786C50765161726257524D305745333639667A677631504C536D3331664373534D566E3278655633732B5968744C62552F6D615A0A48703939784934362F5A61576E767A4D6F647754665A4E42466A47674F33647A506939423955364D417278486768437A3930323058665A5A497839685245662B75656B3652630A6F642F42364C736863313679736B494F7372316832654377793278513935372B4E345054456F5772695338536855526B366D47744D6248455263494C584347323358515171720A2F424D5645356A6C4278366F36554C3364622B415A634177496B4352614C366373325A4C512B61646F3450487833536466554E584F65776963445750316578747341523867620A484375485872646D757471444D4F5A4A387266364B6D3841594C4F703675615765695450617858746337305A6868657567456331356C4169594C787238452B653274756C6F380A535467395868426A4F6D394E534B5A5442546854525245535877535353635531686A4C66562F5538635964393045415A453959336D4A492B3634525467496D76707A58796A430A56516A3434732B6D756D316E344D6E6144696855757341492B4C775A69455A34796A6A35634772384F424E57754276356E56684E2B447750366854414B456D693362305A53310A7072416A59544536584F314C51447A49625A594A5A5638575532574D6567587278727864455A6E49514A646C4372326D396D41345A714330353830425A32704144377336584B0A2B2B77435748756E6D456A6A6A317254487945484651595632394F3072584B79777732496D777435566B62456D49566B354E2B796C5632794F682B6F4E46304C7A736F594D370A74744A3859745161346B4E72666A38634B36776D7A6F4270432F326132686A51666E6A5A6F4E316739396834344663624B4B775A46437A7368624C7632437761696A594863620A776C4935304468754E35756A70656F50353658485168466A48316D6652463453547242347A6E653642494E526D6A595353702B6C31613371324634654854627478484E6542390A455455434E5038363178784254466F6B7244542B6D4959717A61795773416157316D5256767265532F6574716836634741584478352F492F356E3777454376504C52393258340A647A576E6E42673434634C4C62534D4E496A6F2F4D47746B3652657257512B6D31704359706D57544568616A6E786479796A4C396C54666A324C7162354D4D7A3944727359550A6E34514F7A6456437648336D38712B37456733392F3456512B6B672F425277517148596B513077386B43784757786D65753854755039374E6D496356354156646F4958464C6B0A645836516D426F5043424D7956724747592F714C43526E7945633434685A69774B34774D694F736C7963436A7878383161724B68364852735A50504E53366E413835777462700A30594235454645344E75384E54304D5971446E7646346C55776765737838784E7678496E674A57697744344C4D5733447673714736614246316D527879614F704F70752B66540A734A54413D3D0A2D2D2D2D2D454E44204F50454E5353482050524956415445204B45592D2D2D2D2D0A,
        'application/octet-stream', null, 'sshkey-passphrase', 0);

insert into tbl_ilds_transfer_site (id, created_at, last_modified_at, version, compression_password, credential_type,
                                    destination_type, encryption_key_name, file_rename_suffix, ip, mft_transfer_site_id,
                                    port, remote_path, username, trigger_required, dispatcher, status, credential_id)
values (1, null, null, 1, null, 2, 1, null, null, '172.18.0.3', null, 22, '/upload', 'foo', 0, null, 0, 1),
       (2, null, null, 1, null, 0, 1, null, null, '172.18.0.4', null, 22, '/upload', 'foo', 0, null, 0, 2);
