use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::ptr;
use std::collections::HashMap;
use std::sync::Mutex;
use once_cell::sync::Lazy;

// Global state for MLS groups and key stores
static GROUPS: Lazy<Mutex<HashMap<String, GroupState>>> = Lazy::new(|| Mutex::new(HashMap::new()));

#[derive(Default)]
struct GroupState {
    group_id: String,
    epoch: u64,
    members: Vec<String>,
    group_key: Vec<u8>,
}

#[no_mangle]
pub extern "C" fn mls_create_group() -> *mut c_char {
    let group_id = uuid::Uuid::new_v4().to_string();
    
    let mut groups = GROUPS.lock().unwrap();
    groups.insert(group_id.clone(), GroupState {
        group_id: group_id.clone(),
        epoch: 1,
        members: vec![],
        group_key: vec![0x42; 32], // Placeholder key - will be replaced with real MLS key
    });
    
    let result = format!("{{\"group_id\":\"{}\",\"epoch\":1,\"status\":\"created\"}}", group_id);
    let c_string = CString::new(result).unwrap();
    c_string.into_raw()
}

#[no_mangle]
pub extern "C" fn mls_join_group(group_id: *const c_char) -> *mut c_char {
    if group_id.is_null() {
        return ptr::null_mut();
    }
    
    let group_id_str = unsafe { CStr::from_ptr(group_id).to_string_lossy().to_string() };
    let mut groups = GROUPS.lock().unwrap();
    
    if let Some(group) = groups.get_mut(&group_id_str) {
        group.epoch += 1;
        let result = format!("{{\"group_id\":\"{}\",\"epoch\":{},\"status\":\"joined\"}}", 
                           group_id_str, group.epoch);
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    } else {
        let result = "{\"error\":\"Group not found\"}";
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    }
}

#[no_mangle]
pub extern "C" fn mls_encrypt_message(
    group_id: *const c_char,
    message: *const c_char
) -> *mut c_char {
    if group_id.is_null() || message.is_null() {
        return ptr::null_mut();
    }
    
    let group_id_str = unsafe { CStr::from_ptr(group_id).to_string_lossy().to_string() };
    let message_str = unsafe { CStr::from_ptr(message).to_string_lossy().to_string() };
    
    let groups = GROUPS.lock().unwrap();
    if let Some(group) = groups.get(&group_id_str) {
        // Simple XOR encryption with group key (placeholder for real MLS encryption)
        let encrypted: Vec<u8> = message_str.bytes()
            .zip(group.group_key.iter().cycle())
            .map(|(b, k)| b ^ k)
            .collect();
        
        let result = format!("{{\"encrypted\":\"{}\",\"group_id\":\"{}\",\"epoch\":{}}}", 
                           base64::encode(encrypted), group_id_str, group.epoch);
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    } else {
        let result = "{\"error\":\"Group not found\"}";
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    }
}

#[no_mangle]
pub extern "C" fn mls_decrypt_message(
    group_id: *const c_char,
    encrypted_message: *const c_char
) -> *mut c_char {
    if group_id.is_null() || encrypted_message.is_null() {
        return ptr::null_mut();
    }
    
    let group_id_str = unsafe { CStr::from_ptr(group_id).to_string_lossy().to_string() };
    let encrypted_str = unsafe { CStr::from_ptr(encrypted_message).to_string_lossy().to_string() };
    
    let groups = GROUPS.lock().unwrap();
    if let Some(group) = groups.get(&group_id_str) {
        // Simple XOR decryption with group key (placeholder for real MLS decryption)
        let encrypted_bytes = match base64::decode(encrypted_str) {
            Ok(bytes) => bytes,
            Err(_) => {
                let result = "{\"error\":\"Invalid base64 encoding\"}";
                let c_string = CString::new(result).unwrap();
                return c_string.into_raw();
            }
        };
        
        let decrypted: Vec<u8> = encrypted_bytes
            .iter()
            .zip(group.group_key.iter().cycle())
            .map(|(b, k)| b ^ k)
            .collect();
        
        let decrypted_str = String::from_utf8_lossy(&decrypted);
        let result = format!("{{\"decrypted\":\"{}\",\"group_id\":\"{}\",\"epoch\":{}}}", 
                           decrypted_str, group_id_str, group.epoch);
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    } else {
        let result = "{\"error\":\"Group not found\"}";
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    }
}

#[no_mangle]
pub extern "C" fn mls_get_group_info(group_id: *const c_char) -> *mut c_char {
    if group_id.is_null() {
        return ptr::null_mut();
    }
    
    let group_id_str = unsafe { CStr::from_ptr(group_id).to_string_lossy().to_string() };
    let groups = GROUPS.lock().unwrap();
    
    if let Some(group) = groups.get(&group_id_str) {
        let result = format!("{{\"group_id\":\"{}\",\"epoch\":{},\"member_count\":{}}}", 
                           group.group_id, group.epoch, group.members.len());
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    } else {
        let result = "{\"error\":\"Group not found\"}";
        let c_string = CString::new(result).unwrap();
        c_string.into_raw()
    }
}

#[no_mangle]
pub extern "C" fn mls_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}
