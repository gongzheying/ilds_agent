delete
from tbl_ilds_transfer_site;

delete
from tbl_ilds_transfer_credentials;

insert into tbl_ilds_transfer_credentials (id, created_at, last_modified_at, version, private_key_name,
                                           private_key_passphrase, password, title)
values (1, null, null, 1, null, null, 'password', 'unit test');

insert into tbl_ilds_transfer_site (id, created_at, last_modified_at, version, compression_password, credential_type,
                                    destination_type, encryption_key_name, file_rename_suffix, ip, mft_transfer_site_id,
                                    port, remote_path, username, trigger_required, dispatcher, status, credential_id)
values (1, null, null, 1, null, 2, 1, null, null, '172.18.0.3', null, 22, '/upload', 'foo', 0, null, 0, 1),
       (2, null, null, 1, null, 2, 1, null, null, '172.18.0.4', null, 22, '/upload_err', 'foo', 0, null, 0, 1);
